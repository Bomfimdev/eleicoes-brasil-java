import { Component, HostListener, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs/operators';
import { ApiService } from './core/api.service';
import { fmtClockBrt } from './core/format';
import { LiveSessionService } from './core/live-session.service';
import { ElectionSummary } from './core/models';

interface NavItem {
  path: string;
  label: string;
  exact?: boolean;
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, FormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  readonly live = inject(LiveSessionService);

  readonly fmtClockBrt = fmtClockBrt;

  readonly elections = signal<ElectionSummary[]>([]);
  readonly moreOpen = signal(false);
  readonly theme = signal<'dark' | 'light'>('dark');

  readonly nav: NavItem[] = [
    { path: '/', label: 'Visão geral', exact: true },
    { path: '/estados', label: 'Estados' },
    { path: '/eleitos', label: 'Eleitos' },
    { path: '/ao-vivo', label: 'Ao vivo' },
    { path: '/historico', label: 'Histórico' },
    { path: '/comparar', label: 'Comparar' },
  ];

  ngOnInit(): void {
    this.live.start();
    this.api.elections().subscribe({
      next: (list) => {
        const preferred = list.filter((e) => !e.demo);
        this.elections.set(preferred.length > 0 ? preferred : list);
        const current = this.live.roundSlug();
        const hasCurrent = this.elections().some((e) => e.rounds.some((r) => r.slug === current));
        if (!hasCurrent) {
          const first = this.elections()[0]?.rounds[0]?.slug;
          if (first) this.live.switchRound(first);
        }
      },
      error: () => this.elections.set([]),
    });
    const saved = document.documentElement.dataset['theme'];
    this.theme.set(saved === 'light' ? 'light' : 'dark');
    this.router.events.pipe(filter((e) => e instanceof NavigationEnd)).subscribe(() => {
      this.moreOpen.set(false);
    });
  }

  @HostListener('window:keydown', ['$event'])
  onKey(e: KeyboardEvent): void {
    if (e.key === 'Escape') this.moreOpen.set(false);
  }

  onRoundChange(slug: string): void {
    this.live.switchRound(slug);
    void this.router.navigateByUrl('/');
  }

  toggleTheme(): void {
    const next = this.theme() === 'light' ? 'dark' : 'light';
    this.theme.set(next);
    document.documentElement.dataset['theme'] = next;
    try {
      localStorage.setItem('eleicoes:theme', next);
    } catch {
      /* ignore */
    }
  }

  statusTone(): 'live' | 'warn' | 'muted' | 'ink' {
    switch (this.live.phase()) {
      case 'ao_vivo':
        return 'live';
      case 'atrasado':
        return 'warn';
      case 'encerrado':
        return 'ink';
      default:
        return 'muted';
    }
  }

  roundOptionLabel(e: ElectionSummary, r: { round: number; demo: boolean; slug: string }): string {
    const tag = e.demo ? 'Demo' : e.slug.startsWith('replay-') ? 'Replay' : String(e.year);
    return `${tag} · ${r.round}º turno`;
  }
}
