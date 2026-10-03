import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { fmtClock, fmtInt, fmtPct } from '../../core/format';
import { LiveSessionService } from '../../core/live-session.service';
import { CityRowDto, ResultDto, StateDetailDto } from '../../core/models';

@Component({
  selector: 'app-state-page',
  standalone: true,
  imports: [RouterLink],
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
  readonly result = signal<ResultDto | null>(null);
  readonly cities = signal<CityRowDto[]>([]);
  readonly error = signal<string | null>(null);
  readonly loading = signal(true);

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

  private load(uf: string): void {
    this.loading.set(true);
    this.error.set(null);
    const slug = this.live.roundSlug();
    const v = this.live.version();
    this.api.state(slug, uf, v).subscribe({
      next: (d) => {
        this.detail.set(d);
        this.loading.set(false);
        this.api.stateResults(slug, uf, 'presidente', v).subscribe({
          next: (r) => this.result.set(r),
          error: () => this.result.set(null),
        });
        this.api.cities(slug, uf, { pageSize: 40 }).subscribe({
          next: (page) => this.cities.set(page.items),
          error: () => this.cities.set([]),
        });
      },
      error: () => {
        this.error.set('Não foi possível carregar este estado.');
        this.loading.set(false);
      },
    });
  }
}
