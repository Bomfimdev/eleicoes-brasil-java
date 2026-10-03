import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { fmtClock, fmtInt, fmtPct } from '../../core/format';
import { LiveSessionService } from '../../core/live-session.service';

@Component({
  selector: 'app-national-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './national.page.html',
  styleUrl: './national.page.scss',
})
export class NationalPage implements OnInit {
  readonly live = inject(LiveSessionService);
  readonly fmtPct = fmtPct;
  readonly fmtInt = fmtInt;
  readonly fmtClock = fmtClock;

  ngOnInit(): void {
    this.live.start();
  }

  phaseLabel(): string {
    switch (this.live.phase()) {
      case 'ao_vivo':
        return 'Ao vivo';
      case 'encerrado':
        return 'Encerrado';
      case 'atrasado':
        return 'Atrasado';
      case 'offline':
        return 'Offline';
      default:
        return 'Aguardando início';
    }
  }
}
