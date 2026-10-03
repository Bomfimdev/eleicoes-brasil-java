import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ElectionSummary, OverviewDto, ResultDto, StateDetailDto } from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiUrl;

  elections(): Observable<ElectionSummary[]> {
    return this.http.get<ElectionSummary[]>(`${this.base}/api/elections`);
  }

  overview(roundSlug: string, version?: number): Observable<OverviewDto> {
    const v = version != null ? `?v=${version}` : '';
    return this.http.get<OverviewDto>(`${this.base}/api/elections/${roundSlug}/overview${v}`);
  }

  state(roundSlug: string, uf: string, version?: number): Observable<StateDetailDto> {
    const v = version != null ? `?v=${version}` : '';
    return this.http.get<StateDetailDto>(
      `${this.base}/api/elections/${roundSlug}/states/${uf}${v}`,
    );
  }

  stateResults(roundSlug: string, uf: string, office?: string, version?: number): Observable<ResultDto> {
    const params = new URLSearchParams();
    if (office) params.set('office', office);
    if (version != null) params.set('v', String(version));
    const q = params.toString() ? `?${params}` : '';
    return this.http.get<ResultDto>(
      `${this.base}/api/elections/${roundSlug}/states/${uf}/results${q}`,
    );
  }

  loadArchive(path: string): Observable<OverviewDto> {
    return this.http.get<OverviewDto>(path);
  }
}
