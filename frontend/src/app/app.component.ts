import { Component, HostListener, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs/operators';
import { ApiService } from './core/api.service';
import { fmtClockBrt, fmtPct } from './core/format';
import { LiveSessionService } from './core/live-session.service';
import { ElectionSummary, StateRowDto } from './core/models';

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
  readonly fmtPct = fmtPct;

  readonly elections = signal<ElectionSummary[]>([]);
  readonly searchOpen = signal(false);
  readonly moreOpen = signal(false);
  readonly searchQuery = signal('');
  readonly theme = signal<'dark' | 'light'>('dark');

  readonly nav: NavItem[] = [
    { path: '/', label: 'Visão geral', exact: true },
    { path: '/estados', label: 'Estados' },
    { path: '/ao-vivo', label: 'Ao vivo' },
    { path: '/historico', label: 'Histórico' },
    { path: '/comparar', label: 'Comparar' },
  ];

  readonly filteredStates = computed(() => {
    const q = this.searchQuery().trim().toLowerCase();
    const states = this.live.overview()?.states ?? [];
    if (!q) return states.slice(0, 12);
    return states
      .filter((s) => s.uf.toLowerCase().includes(q) || s.name.toLowerCase().includes(q))
      .slice(0, 20);
  });

  ngOnInit(): void {
    this.live.start();
    this.api.elections().subscribe({
      next: (list) => this.elections.set(list),
      error: () => this.elections.set([]),
    });
    const saved = document.documentElement.dataset['theme'];
    this.theme.set(saved === 'light' ? 'light' : 'dark');
    this.router.events.pipe(filter((e) => e instanceof NavigationEnd)).subscribe(() => {
      this.moreOpen.set(false);
      this.searchOpen.set(false);
    });
  }

  @HostListener('window:keydown', ['$event'])
  onKey(e: KeyboardEvent): void {
    const typing =
      e.target instanceof HTMLElement && /^(INPUT|TEXTAREA|SELECT)$/.test(e.target.tagName);
    if ((e.key === 'k' && (e.metaKey || e.ctrlKey)) || (e.key === '/' && !typing)) {
      e.preventDefault();
      this.openSearch();
    }
    if (e.key === 'Escape') {
      this.searchOpen.set(false);
      this.moreOpen.set(false);
    }
  }

  openSearch(): void {
    this.searchOpen.set(true);
    this.searchQuery.set('');
    setTimeout(() => document.getElementById('search-input')?.focus(), 0);
  }

  closeSearch(): void {
    this.searchOpen.set(false);
  }

  goState(s: StateRowDto): void {
    this.closeSearch();
    void this.router.navigate(['/estado', s.uf]);
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
