import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription, switchMap } from 'rxjs';
import { EvolutionChartComponent } from '../../components/evolution-chart/evolution-chart.component';
import { ApiService } from '../../core/api.service';
import { fmtClock, fmtInt, fmtPct } from '../../core/format';
import { LiveSessionService } from '../../core/live-session.service';
import { CityDetailDto, SeriesDto } from '../../core/models';

@Component({
  selector: 'app-city-page',
  standalone: true,
  imports: [RouterLink, EvolutionChartComponent],
  templateUrl: './city.page.html',
  styleUrl: './city.page.scss',
})
export class CityPage implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(ApiService);
  private readonly live = inject(LiveSessionService);
  private sub: Subscription | null = null;

  readonly detail = signal<CityDetailDto | null>(null);
  readonly series = signal<SeriesDto | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly fmtPct = fmtPct;
  readonly fmtInt = fmtInt;
  readonly fmtClock = fmtClock;

  ngOnInit(): void {
    this.live.start();
    this.sub = this.route.paramMap
      .pipe(
        switchMap((p) => {
          const uf = p.get('uf')!;
          const code = p.get('code')!;
          this.loading.set(true);
          return this.api.city(this.live.roundSlug(), uf, code);
        }),
      )
      .subscribe({
        next: (d) => {
          this.detail.set(d);
          this.loading.set(false);
          this.error.set(null);
          const area = `${d.uf.toLowerCase()}-${d.city.code}`;
          this.api.series(this.live.roundSlug(), 'presidente', area).subscribe({
            next: (s) => this.series.set(s),
            error: () => this.series.set(null),
          });
        },
        error: () => {
          this.error.set('Não foi possível carregar o município.');
          this.loading.set(false);
        },
      });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }
}
