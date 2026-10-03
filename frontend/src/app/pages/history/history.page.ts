import { Component, OnDestroy, OnInit, effect, inject, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { fmtClock, fmtPct } from '../../core/format';
import { LiveSessionService } from '../../core/live-session.service';
import { TimelineAtDto, TimelineDto } from '../../core/models';

@Component({
  selector: 'app-history-page',
  standalone: true,
  templateUrl: './history.page.html',
  styleUrl: './history.page.scss',
})
export class HistoryPage implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);
  readonly live = inject(LiveSessionService);

  readonly timeline = signal<TimelineDto | null>(null);
  readonly snapshot = signal<TimelineAtDto | null>(null);
  readonly index = signal(0);
  readonly playing = signal(false);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  private playTimer: ReturnType<typeof setInterval> | null = null;
  private atSub: Subscription | null = null;

  readonly fmtPct = fmtPct;
  readonly fmtClock = fmtClock;

  constructor() {
    effect(() => {
      this.live.roundSlug();
      this.loadTimeline();
    });
  }

  ngOnInit(): void {
    this.live.start();
  }

  ngOnDestroy(): void {
    this.stopPlay();
    this.atSub?.unsubscribe();
  }

  points() {
    return this.timeline()?.points ?? [];
  }

  currentAt(): string | null {
    return this.points()[this.index()]?.at ?? null;
  }

  onSlider(value: string): void {
    this.stopPlay();
    this.index.set(Number(value));
    this.loadAt();
  }

  togglePlay(): void {
    if (this.playing()) {
      this.stopPlay();
      return;
    }
    if (this.index() >= this.points().length - 1) this.index.set(0);
    this.playing.set(true);
    this.playTimer = setInterval(() => {
      const next = this.index() + 1;
      if (next >= this.points().length - 1) {
        this.index.set(this.points().length - 1);
        this.stopPlay();
      } else {
        this.index.set(next);
      }
      this.loadAt();
    }, 350);
  }

  private stopPlay(): void {
    this.playing.set(false);
    if (this.playTimer) {
      clearInterval(this.playTimer);
      this.playTimer = null;
    }
  }

  private loadTimeline(): void {
    this.loading.set(true);
    this.api.timeline(this.live.roundSlug()).subscribe({
      next: (t) => {
        this.timeline.set(t);
        this.loading.set(false);
        this.error.set(null);
        const last = Math.max(0, t.points.length - 1);
        this.index.set(last);
        if (t.points.length) this.loadAt();
        else this.snapshot.set(null);
      },
      error: () => {
        this.error.set('Não foi possível carregar o histórico.');
        this.loading.set(false);
      },
    });
  }

  private loadAt(): void {
    const at = this.currentAt();
    if (!at) return;
    this.atSub?.unsubscribe();
    this.atSub = this.api.timelineAt(this.live.roundSlug(), at).subscribe({
      next: (s) => this.snapshot.set(s),
      error: () => this.snapshot.set(null),
    });
  }
}
