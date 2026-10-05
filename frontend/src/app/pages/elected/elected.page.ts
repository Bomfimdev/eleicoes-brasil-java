import { Component, OnInit, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, catchError, debounceTime, finalize, of, switchMap } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { fmtInt, fmtPct } from '../../core/format';
import { LiveSessionService } from '../../core/live-session.service';
import { ElectedDto, ElectedOfficeGroupDto } from '../../core/models';

const TABS: { slug: string; label: string }[] = [
  { slug: 'governador', label: 'Governador' },
  { slug: 'senador', label: 'Senador' },
  { slug: 'deputado-federal', label: 'Dep. Federal' },
  { slug: 'deputado-estadual', label: 'Dep. Estadual' },
];

@Component({
  selector: 'app-elected-page',
  standalone: true,
  templateUrl: './elected.page.html',
  styleUrl: './elected.page.scss',
})
export class ElectedPage implements OnInit {
  private readonly api = inject(ApiService);
  readonly live = inject(LiveSessionService);
  private readonly reload$ = new Subject<void>();

  readonly tabs = TABS;
  readonly tab = signal(TABS[0].slug);
  readonly data = signal<ElectedDto | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly fmtPct = fmtPct;
  readonly fmtInt = fmtInt;

  readonly group = computed<ElectedOfficeGroupDto | null>(() => {
    const d = this.data();
    if (!d) return null;
    const slug = this.tab();
    if (slug === 'deputado-estadual') {
      const est = d.offices.find((o) => o.officeSlug === 'deputado-estadual');
      const dist = d.offices.find((o) => o.officeSlug === 'deputado-distrital');
      if (!est && !dist) return null;
      const people = [...(est?.people ?? []), ...(dist?.people ?? [])];
      return {
        officeSlug: 'deputado-estadual',
        officeName: 'Deputado Estadual / Distrital',
        count: people.length,
        people,
      };
    }
    return d.offices.find((o) => o.officeSlug === slug) ?? null;
  });

  constructor() {
    this.reload$
      .pipe(
        debounceTime(250),
        switchMap(() => {
          if (!this.data()) {
            this.loading.set(true);
          }
          return this.api.elected(this.live.roundSlug()).pipe(
            catchError(() => {
              if (!this.data()) {
                this.error.set('Nao foi possivel carregar os eleitos.');
              }
              return of(null);
            }),
            finalize(() => this.loading.set(false)),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((d) => {
        if (!d) return;
        this.data.set(d);
        this.error.set(null);
      });

    effect(() => {
      this.live.roundSlug();
      this.live.version();
      this.reload$.next();
    });
  }

  ngOnInit(): void {
    this.live.start();
  }

  setTab(slug: string): void {
    this.tab.set(slug);
  }

  statusLabel(status: string | null): string {
    if (!status) return 'Eleito';
    const s = status.toLowerCase().normalize('NFD').replace(/\p{M}/gu, '');
    if (/nao\s+eleito/.test(s) || /2\s*o?\s*turno/.test(s)) return '';
    if (/^eleito/.test(s) || s.includes('eleito por')) return 'Eleito';
    return status;
  }
}
