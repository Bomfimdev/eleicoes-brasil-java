import { Component, Input, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import brazil from '@svg-maps/brazil';
import { fmtPct } from '../../core/format';
import { StateRowDto } from '../../core/models';

type Mode = 'progress' | 'leader';

const MAP = brazil as {
  viewBox: string;
  locations: { id: string; name: string; path: string }[];
};

@Component({
  selector: 'app-brazil-map',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './brazil-map.component.html',
  styleUrl: './brazil-map.component.scss',
})
export class BrazilMapComponent {
  @Input({ required: true }) states: StateRowDto[] = [];
  @Input() allowLeader = true;

  private readonly nav = inject(Router);

  readonly mode = signal<Mode>('progress');
  readonly active = signal<string | null>(null);
  readonly pinned = signal<string | null>(null);
  readonly fmtPct = fmtPct;
  readonly map = MAP;

  readonly byUf = computed(() => {
    const m = new Map<string, StateRowDto>();
    for (const s of this.states) m.set(s.uf.toLowerCase(), s);
    return m;
  });

  readonly shown = computed(() => {
    const key = this.active() ?? this.pinned();
    return key ? (this.byUf().get(key) ?? null) : null;
  });

  setMode(m: Mode): void {
    this.mode.set(m);
  }

  fill(locId: string): string {
    const s = this.byUf().get(locId);
    if (!s?.progress || s.progress.status === 'not-started') return 'var(--surface-2)';
    if (this.mode() === 'leader') {
      if (!s.leaderName) return 'var(--surface-2)';
      const hue = Math.abs(hash(s.leaderName)) % 360;
      return `hsl(${hue} 45% 42%)`;
    }
    const p = Math.max(0, Math.min(100, s.progress.countedPct ?? 0));
    return `color-mix(in srgb, var(--live) ${p}%, var(--surface-2))`;
  }

  onEnter(id: string): void {
    this.active.set(id);
  }

  onLeave(): void {
    this.active.set(null);
  }

  onActivate(id: string, event: Event): void {
    const isTouch = matchMedia('(pointer: coarse)').matches;
    if (isTouch && this.pinned() !== id) {
      event.preventDefault();
      this.pinned.set(id);
      this.active.set(id);
      return;
    }
    void this.nav.navigate(['/estado', id.toUpperCase()]);
  }

  onKey(id: string, event: KeyboardEvent): void {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      void this.nav.navigate(['/estado', id.toUpperCase()]);
    }
  }
}

function hash(s: string): number {
  let h = 0;
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) | 0;
  return h;
}
