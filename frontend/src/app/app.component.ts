import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { environment } from '../environments/environment';

type HealthResponse = {
  status: string;
  service: string;
  mode: string;
  timestamp: string;
};

@Component({
  selector: 'app-root',
  standalone: true,
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent implements OnInit {
  private readonly http = inject(HttpClient);

  readonly title = 'Eleições Brasil';
  readonly health = signal<HealthResponse | null>(null);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.http.get<HealthResponse>(`${environment.apiUrl}/api/health`).subscribe({
      next: (response) => {
        this.health.set(response);
        this.error.set(null);
      },
      error: () => {
        this.health.set(null);
        this.error.set('API offline — verifique o backend (local :8080 ou Render).');
      },
    });
  }
}
