import { Injectable, OnDestroy, inject } from '@angular/core';
import { Subject } from 'rxjs';
import { environment } from '../../environments/environment';

export type RealtimeEvent = {
  electionId: string;
  version: number;
  timestamp?: string;
};

@Injectable({ providedIn: 'root' })
export class RealtimeService implements OnDestroy {
  private source: EventSource | null = null;
  private roundSlug: string | null = null;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private reconnectAttempt = 0;
  private readonly versions = new Subject<RealtimeEvent>();

  readonly version$ = this.versions.asObservable();

  connect(roundSlug: string): void {
    if (this.roundSlug === roundSlug && this.source?.readyState === EventSource.OPEN) {
      return;
    }
    this.disconnect();
    this.roundSlug = roundSlug;
    this.open();
  }

  disconnect(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    this.source?.close();
    this.source = null;
  }

  ngOnDestroy(): void {
    this.disconnect();
  }

  private open(): void {
    if (!this.roundSlug) return;
    const url = `${environment.apiUrl}/api/realtime/elections/${this.roundSlug}`;
    const es = new EventSource(url);
    this.source = es;

    es.addEventListener('ready', (ev) => this.emit(ev as MessageEvent));
    es.addEventListener('version', (ev) => this.emit(ev as MessageEvent));

    es.onopen = () => {
      this.reconnectAttempt = 0;
    };

    es.onerror = () => {
      es.close();
      this.source = null;
      this.scheduleReconnect();
    };
  }

  private emit(ev: MessageEvent): void {
    try {
      const data = JSON.parse(ev.data) as RealtimeEvent;
      if (typeof data.version === 'number') {
        this.versions.next(data);
      }
    } catch {
      // ignore malformed
    }
  }

  private scheduleReconnect(): void {
    if (this.reconnectTimer) return;
    const delay = Math.min(30_000, 1000 * 2 ** this.reconnectAttempt);
    this.reconnectAttempt++;
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      this.open();
    }, delay);
  }
}
