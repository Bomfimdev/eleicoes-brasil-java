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

  private readonly dateFmt = new Intl.DateTimeFormat('pt-BR', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
    timeZone: 'UTC',
  });

  ngOnInit(): void {
    this.live.start();
  }

  formatDate(isoDate: string): string {
    try {
      return this.dateFmt.format(new Date(`${isoDate}T12:00:00Z`));
    } catch {
      return isoDate;
    }
  }
}
