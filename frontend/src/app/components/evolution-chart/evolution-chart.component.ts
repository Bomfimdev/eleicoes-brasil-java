import {
  Component,
  ElementRef,
  HostListener,
  Input,
  OnChanges,
  ViewChild,
  signal,
} from '@angular/core';
import { fmtClock, fmtPct } from '../../core/format';
import { SeriesDto } from '../../core/models';

const H = 280;
const PAD = { top: 16, right: 16, bottom: 28, left: 40 };

@Component({
  selector: 'app-evolution-chart',
  standalone: true,
  templateUrl: './evolution-chart.component.html',
  styleUrl: './evolution-chart.component.scss',
})
export class EvolutionChartComponent implements OnChanges {
  @Input({ required: true }) series!: SeriesDto;
  @Input() majority = true;

  @ViewChild('box', { static: false }) box?: ElementRef<HTMLDivElement>;

  readonly width = signal(0);
  readonly hover = signal<number | null>(null);
  readonly fmtPct = fmtPct;
  readonly fmtClock = fmtClock;
  readonly Math = Math;
  readonly H = H;
  readonly PAD = PAD;

  points: SeriesDto['points'] = [];
  lo = 0;
  hi = 100;
  yTicks: number[] = [];
  xTicks: number[] = [];

  ngOnChanges(): void {
    this.points = (this.series?.points ?? []).filter((p) =>
      Object.values(p.values).some((v) => v != null && v > 0),
    );
    this.computeScale();
    queueMicrotask(() => this.measure());
  }

  get ready(): boolean {
    return this.points.length >= 2;
  }

  @HostListener('window:resize')
  onResize(): void {
    this.measure();
  }

  measure(): void {
    const el = this.box?.nativeElement;
    if (!el) return;
    this.width.set(Math.max(260, Math.floor(el.clientWidth)));
    this.computeScale();
  }

  onMove(clientX: number): void {
    const el = this.box?.nativeElement;
    if (!el || !this.ready) return;
    const rect = el.getBoundingClientRect();
    const t0 = Date.parse(this.points[0].at);
    const t1 = Date.parse(this.points[this.points.length - 1].at);
    const iw = this.width() - PAD.left - PAD.right;
    const t = t0 + ((clientX - rect.left - PAD.left) / Math.max(1, iw)) * (t1 - t0);
    let best = 0;
    for (let i = 1; i < this.points.length; i++) {
      if (
        Math.abs(Date.parse(this.points[i].at) - t) <
        Math.abs(Date.parse(this.points[best].at) - t)
      ) {
        best = i;
      }
    }
    this.hover.set(best);
  }

  x(iso: string): number {
    return this.xAt(Date.parse(iso));
  }

  xAt(ms: number): number {
    const t0 = Date.parse(this.points[0].at);
    const t1 = Date.parse(this.points[this.points.length - 1].at);
    const iw = this.width() - PAD.left - PAD.right;
    return PAD.left + ((ms - t0) / Math.max(1, t1 - t0)) * iw;
  }

  fmtClockMs(ms: number): string {
    return fmtClock(new Date(ms).toISOString());
  }

  y(v: number): number {
    const ih = H - PAD.top - PAD.bottom;
    return PAD.top + (1 - (v - this.lo) / Math.max(1, this.hi - this.lo)) * ih;
  }

  pathFor(key: string): string {
    const pts = this.points
      .map((p) => {
        const v = p.values[key];
        return v == null ? null : `${this.x(p.at)},${this.y(v)}`;
      })
      .filter((x): x is string => x != null);
    return pts.length ? `M ${pts.join(' L ')}` : '';
  }

  hoverPoint() {
    const i = this.hover();
    return i == null ? null : this.points[i];
  }

  private computeScale(): void {
    if (!this.ready) return;
    const values = this.points.flatMap((p) =>
      Object.values(p.values).filter((v): v is number => v != null),
    );
    let lo = Math.max(0, Math.floor(Math.min(...values) - 2));
    let hi = Math.min(100, Math.ceil(Math.max(...values) + 2));
    if (this.majority && lo < 50 && hi > 42) hi = Math.max(hi, 52);
    const step = hi - lo > 40 ? 10 : hi - lo > 16 ? 5 : hi - lo > 6 ? 2 : 1;
    lo = Math.floor(lo / step) * step;
    hi = Math.ceil(hi / step) * step;
    this.lo = lo;
    this.hi = hi;
    this.yTicks = Array.from({ length: Math.round((hi - lo) / step) + 1 }, (_, i) => lo + i * step);
    const t0 = Date.parse(this.points[0].at);
    const t1 = Date.parse(this.points[this.points.length - 1].at);
    const n = this.width() < 480 ? 3 : 5;
    this.xTicks = Array.from({ length: n }, (_, i) => t0 + ((t1 - t0) * i) / (n - 1));
  }
}
