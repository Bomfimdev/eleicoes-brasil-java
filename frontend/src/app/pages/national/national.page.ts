import { Component, OnDestroy, OnInit, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { BrazilMapComponent } from '../../components/brazil-map/brazil-map.component';
import { EvolutionChartComponent } from '../../components/evolution-chart/evolution-chart.component';
import { ApiService } from '../../core/api.service';
import { fmtClock, fmtInt, fmtPct } from '../../core/format';
import { LiveSessionService } from '../../core/live-session.service';
import { SeriesDto } from '../../core/models';

@Component({
  selector: 'app-national-page',
  standalone: true,
  imports: [RouterLink, BrazilMapComponent, EvolutionChartComponent],
  templateUrl: './national.page.html',
  styleUrl: './national.page.scss',
})
export class NationalPage implements OnInit, OnDestroy {
  readonly live = inject(LiveSessionService);
  private readonly api = inject(ApiService);
  readonly fmtPct = fmtPct;
  readonly fmtInt = fmtInt;
  readonly fmtClock = fmtClock;

  readonly tab = signal<'mapa' | 'evolucao'>('mapa');
  readonly series = signal<SeriesDto | null>(null);
  private seriesSub: Subscription | null = null;

  private readonly dateFmt = new Intl.DateTimeFormat('pt-BR', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
    timeZone: 'UTC',
  });

  constructor() {
    effect(() => {
      const slug = this.live.roundSlug();
      this.live.version();
      if (this.tab() !== 'evolucao') return;
      this.seriesSub?.unsubscribe();
      this.seriesSub = this.api.series(slug, 'presidente', 'br').subscribe({
        next: (s) => this.series.set(s),
        error: () => this.series.set(null),
      });
    });
  }

  ngOnInit(): void {
    this.live.start();
  }

  ngOnDestroy(): void {
    this.seriesSub?.unsubscribe();
  }

  setTab(t: 'mapa' | 'evolucao'): void {
    this.tab.set(t);
  }

  formatDate(isoDate: string): string {
    try {
      return this.dateFmt.format(new Date(`${isoDate}T12:00:00Z`));
    } catch {
      return isoDate;
    }
  }
}
