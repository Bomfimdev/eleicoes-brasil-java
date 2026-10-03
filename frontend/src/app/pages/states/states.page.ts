import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { fmtPct } from '../../core/format';
import { LiveSessionService } from '../../core/live-session.service';

@Component({
  selector: 'app-states-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './states.page.html',
  styleUrl: './states.page.scss',
})
export class StatesPage implements OnInit {
  readonly live = inject(LiveSessionService);
  readonly fmtPct = fmtPct;

  ngOnInit(): void {
    this.live.start();
  }
}
