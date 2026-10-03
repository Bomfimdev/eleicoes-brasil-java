import { Component, OnDestroy, OnInit, effect, inject, signal } from '@angular/core';
import { Subscription, switchMap, timer } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { ageLabel, fmtClock, fmtInt } from '../../core/format';
import { LiveSessionService } from '../../core/live-session.service';
import { OperationsDto } from '../../core/models';

const STATE_LABEL: Record<string, { label: string; hint: string }> = {
  healthy: { label: 'Saudável', hint: 'Coletando normalmente.' },
  degraded: {
    label: 'Degradado',
    hint: 'A última coleta teve falhas. Os dados podem estar atrasados.',
  },
  offline: { label: 'Desconectado', hint: 'O coletor não responde há mais de 2 minutos.' },
  idle: { label: 'Parado', hint: 'Nenhuma coleta em andamento para esta eleição.' },
  waiting: {
    label: 'Aguardando o TSE',
    hint: 'O TSE ainda não publicou os arquivos desta eleição.',
  },
};

@Component({
  selector: 'app-live-page',
  standalone: true,
  templateUrl: './live.page.html',
  styleUrl: './live.page.scss',
})
export class LivePage implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);
  readonly live = inject(LiveSessionService);

  readonly data = signal<OperationsDto | null>(null);
  readonly error = signal<string | null>(null);
  readonly loading = signal(true);
  readonly now = signal(Date.now());

  private poll: Subscription | null = null;
  private clock: ReturnType<typeof setInterval> | null = null;

  readonly fmtInt = fmtInt;
  readonly fmtClock = fmtClock;
  readonly ageLabel = ageLabel;
  readonly Math = Math;

  constructor() {
    effect(() => {
      this.live.roundSlug();
      this.live.version();
      this.reload();
    });
  }

  ngOnInit(): void {
    this.live.start();
    this.clock = setInterval(() => this.now.set(Date.now()), 1000);
    this.poll = timer(0, 15_000)
      .pipe(switchMap(() => this.api.operations(this.live.roundSlug(), this.live.version())))
      .subscribe({
        next: (d) => {
          this.data.set(d);
          this.loading.set(false);
          this.error.set(null);
        },
        error: () => {
          this.error.set('Não foi possível carregar o painel ao vivo.');
          this.loading.set(false);
        },
      });
  }

  ngOnDestroy(): void {
    this.poll?.unsubscribe();
    if (this.clock) clearInterval(this.clock);
  }

  ingestionMeta(): { label: string; hint: string } {
    const state = this.data()?.ingestion.state ?? 'idle';
    return STATE_LABEL[state] ?? STATE_LABEL['idle'];
  }

  heatSorted(): OperationsDto['heat'] {
    return [...(this.data()?.heat ?? [])].sort((a, b) => b.sections - a.sections || b.updates - a.updates);
  }

  heatMax(): number {
    return Math.max(1, ...this.heatSorted().map((h) => h.sections));
  }

  private reload(): void {
    this.api.operations(this.live.roundSlug(), this.live.version()).subscribe({
      next: (d) => {
        this.data.set(d);
        this.loading.set(false);
        this.error.set(null);
      },
      error: () => {
        /* poll cobre o erro */
      },
    });
  }
}
