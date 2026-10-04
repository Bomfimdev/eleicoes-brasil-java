import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BrazilMapComponent } from '../../components/brazil-map/brazil-map.component';
import { fmtPct } from '../../core/format';
import { LiveSessionService } from '../../core/live-session.service';
import { StateRowDto } from '../../core/models';

@Component({
  selector: 'app-states-page',
  standalone: true,
  imports: [RouterLink, BrazilMapComponent],
  templateUrl: './states.page.html',
  styleUrl: './states.page.scss',
})
export class StatesPage implements OnInit {
  readonly live = inject(LiveSessionService);
  readonly fmtPct = fmtPct;
  readonly openUf = signal<string | null>(null);

  readonly domestic = computed(() => {
    const rows = this.live.overview()?.states.filter((s) => s.uf !== 'ZZ') ?? [];
    return [...rows].sort(
      (a, b) => (b.progress?.countedPct ?? -1) - (a.progress?.countedPct ?? -1),
    );
  });

  readonly exterior = computed(
    () => this.live.overview()?.states.find((s) => s.uf === 'ZZ') ?? null,
  );

  ngOnInit(): void {
    this.live.start();
  }

  onEnter(uf: string): void {
    if (matchMedia('(pointer: coarse)').matches) return;
    this.openUf.set(uf);
  }

  onLeave(): void {
    if (matchMedia('(pointer: coarse)').matches) return;
    this.openUf.set(null);
  }

  onRowClick(s: StateRowDto, event: Event): void {
    const touch = matchMedia('(pointer: coarse)').matches;
    if (touch && this.openUf() !== s.uf) {
      event.preventDefault();
      this.openUf.set(s.uf);
    }
  }

  hasPanel(s: StateRowDto): boolean {
    return (
      (s.presidentTop?.length ?? 0) > 0 ||
      (s.governorTop?.length ?? 0) > 0 ||
      s.progress?.countedPct != null
    );
  }
}
