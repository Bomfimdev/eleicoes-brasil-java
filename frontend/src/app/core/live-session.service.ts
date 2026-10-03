import { Injectable, OnDestroy, computed, inject, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { environment } from '../../environments/environment';
import { ageLabel } from './format';
import { ApiService } from './api.service';
import { OverviewDto, UiPhase } from './models';
import { RealtimeService } from './realtime.service';

@Injectable({ providedIn: 'root' })
export class LiveSessionService implements OnDestroy {
  private readonly api = inject(ApiService);
  private readonly realtime = inject(RealtimeService);
  private sub: Subscription | null = null;
  private clockTimer: ReturnType<typeof setInterval> | null = null;

  readonly roundSlug = signal(environment.roundSlug);
  readonly overview = signal<OverviewDto | null>(null);
  readonly version = signal(0);
  readonly lastOkAt = signal<string | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly now = signal(Date.now());
  readonly archived = signal(false);
  private started = false;

  readonly phase = computed<UiPhase>(() => {
    const o = this.overview();
    if (!o && this.error()) return 'offline';
    if (!o) return 'aguardando';
    if (this.archived()) return 'encerrado';
    const status = o.round.status;
    if (status === 'final') return 'encerrado';
    if (o.ingestion.state === 'offline' || o.ingestion.state === 'degraded') return 'atrasado';
    if (status === 'live' || o.ingestion.state === 'healthy') return 'ao_vivo';
    return 'aguardando';
  });

  readonly lagLabel = computed(() => ageLabel(this.lastOkAt(), this.now()));

  readonly statusLabel = computed(() => {
    switch (this.phase()) {
      case 'ao_vivo':
        return 'Ao vivo';
      case 'encerrado':
        return 'Apuração encerrada';
      case 'atrasado':
        return 'Dados atrasados';
      case 'offline':
        return 'Offline';
      default:
        return 'Aguardando apuração';
    }
  });

  readonly updatedAt = computed(
    () => this.overview()?.progress?.updatedAt ?? this.overview()?.ingestion?.lastSuccessAt ?? null,
  );

  start(): void {
    if (this.started) {
      if (!this.overview()) this.refresh();
      return;
    }
    this.started = true;
    this.refresh();
    this.realtime.connect(this.roundSlug());
    this.sub?.unsubscribe();
    this.sub = this.realtime.version$.subscribe((ev) => {
      this.version.set(ev.version);
      this.refresh(ev.version);
    });
    this.clockTimer = setInterval(() => this.now.set(Date.now()), 5_000);
  }

  switchRound(slug: string): void {
    if (!slug || slug === this.roundSlug()) return;
    this.roundSlug.set(slug);
    this.overview.set(null);
    this.loading.set(true);
    this.error.set(null);
    this.realtime.disconnect();
    this.realtime.connect(slug);
    this.refresh();
  }

  refresh(version?: number): void {
    this.loading.set(true);
    this.api.overview(this.roundSlug(), version).subscribe({
      next: (data) => {
        this.overview.set(data);
        this.lastOkAt.set(new Date().toISOString());
        this.error.set(null);
        this.archived.set(false);
        this.loading.set(false);
      },
      error: () => {
        if (this.overview()) {
          this.archived.set(true);
          this.error.set('Sem conexão — exibindo último snapshot recebido.');
          this.loading.set(false);
          return;
        }
        this.loadArchiveFallback();
      },
    });
  }

  private loadArchiveFallback(): void {
    const path = environment.archivePath;
    if (!path) {
      this.error.set('API indisponível. Tente novamente em instantes.');
      this.loading.set(false);
      return;
    }
    this.api.loadArchive(path).subscribe({
      next: (data) => {
        this.overview.set(data);
        this.archived.set(true);
        this.lastOkAt.set(data.progress?.updatedAt ?? data.ingestion?.lastSuccessAt ?? null);
        this.error.set('Modo arquivado — snapshot estático (API offline).');
        this.loading.set(false);
      },
      error: () => {
        this.error.set('API indisponível e sem arquivo de arquivo local.');
        this.loading.set(false);
      },
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    this.realtime.disconnect();
    if (this.clockTimer) clearInterval(this.clockTimer);
  }
}
