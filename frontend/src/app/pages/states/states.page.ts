import { Component, OnInit, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BrazilMapComponent } from '../../components/brazil-map/brazil-map.component';
import { fmtPct } from '../../core/format';
import { LiveSessionService } from '../../core/live-session.service';

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

  readonly domestic = computed(
    () => this.live.overview()?.states.filter((s) => s.uf !== 'ZZ') ?? [],
  );
  readonly exterior = computed(
    () => this.live.overview()?.states.find((s) => s.uf === 'ZZ') ?? null,
  );

  ngOnInit(): void {
    this.live.start();
  }
}
