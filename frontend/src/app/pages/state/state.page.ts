import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { fmtClock, fmtInt, fmtPct } from '../../core/format';
import { LiveSessionService } from '../../core/live-session.service';
import { CityRowDto, ResultDto, StateDetailDto } from '../../core/models';

type CitySort = 'pct-desc' | 'pct-asc' | 'name-asc' | 'name-desc' | 'leader-desc';

@Component({
  selector: 'app-state-page',
  standalone: true,
  imports: [RouterLink, FormsModule],
  templateUrl: './state.page.html',
  styleUrl: './state.page.scss',
})
export class StatePage implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(ApiService);
  private readonly live = inject(LiveSessionService);
  private sub: Subscription | null = null;

  readonly fmtPct = fmtPct;
  readonly fmtInt = fmtInt;
  readonly fmtClock = fmtClock;

  readonly detail = signal<StateDetailDto | null>(null);
  readonly president = signal<ResultDto | null>(null);
  readonly governor = signal<ResultDto | null>(null);
  readonly cities = signal<CityRowDto[]>([]);
  readonly error = signal<string | null>(null);
  readonly loading = signal(true);
  readonly query = signal('');
  readonly sort = signal<CitySort>('pct-desc');

  readonly exterior = computed(() => (this.detail()?.uf ?? '').toUpperCase() === 'ZZ');

  readonly filteredCities = computed(() => {
    const q = this.query().trim().toLowerCase();
    let rows = this.cities();
    if (q) {
      rows = rows.filter(
        (c) =>
          c.name.toLowerCase().includes(q) ||
          c.code.includes(q) ||
          (c.leaderName?.toLowerCase().includes(q) ?? false),
      );
    }
    const mode = this.sort();
    return [...rows].sort((a, b) => {
      switch (mode) {
        case 'pct-asc':
          return (a.progress?.countedPct ?? -1) - (b.progress?.countedPct ?? -1);
        case 'name-asc':
          return a.name.localeCompare(b.name, 'pt-BR');
        case 'name-desc':
          return b.name.localeCompare(a.name, 'pt-BR');
        case 'leader-desc':
          return (b.leaderPercent ?? -1) - (a.leaderPercent ?? -1);
        case 'pct-desc':
        default:
          return (b.progress?.countedPct ?? -1) - (a.progress?.countedPct ?? -1);
      }
    });
  });

  ngOnInit(): void {
    this.live.start();
    this.sub = this.route.paramMap.subscribe((params) => {
      const uf = params.get('uf');
      if (uf) this.load(uf);
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  onQuery(value: string): void {
    this.query.set(value);
  }

  onSort(value: string): void {
    this.sort.set(value as CitySort);
  }

  private load(uf: string): void {
    this.loading.set(true);
    this.error.set(null);
    this.president.set(null);
    this.governor.set(null);
    this.cities.set([]);
    this.query.set('');
    this.sort.set('pct-desc');
    const slug = this.live.roundSlug();
    const v = this.live.version();
    const isZz = uf.toUpperCase() === 'ZZ';
    this.api.state(slug, uf, v).subscribe({
      next: (d) => {
        this.detail.set(d);
        this.loading.set(false);
        this.api.stateResults(slug, uf, 'presidente', v).subscribe({
          next: (r) => this.president.set(r),
          error: () => this.president.set(null),
        });
        if (!isZz) {
          this.api.stateResults(slug, uf, 'governador', v).subscribe({
            next: (r) => this.governor.set(r),
            error: () => this.governor.set(null),
          });
        }
        this.api.cities(slug, uf, { pageSize: isZz ? 250 : 40 }).subscribe({
          next: (page) => this.cities.set(page.items),
          error: () => this.cities.set([]),
        });
      },
      error: () => {
        this.error.set(isZz ? 'Não foi possível carregar o Exterior.' : 'Não foi possível carregar este estado.');
        this.loading.set(false);
      },
    });
  }
}
