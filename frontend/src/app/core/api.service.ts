import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  CityDetailDto,
  CityPageDto,
  CompareDto,
  ElectedDto,
  ElectionSummary,
  OperationsDto,
  OverviewDto,
  ResultDto,
  SeriesDto,
  StateDetailDto,
  TimelineAtDto,
  TimelineDto,
} from './models';

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

  operations(roundSlug: string, version?: number): Observable<OperationsDto> {
    const v = version != null ? `?v=${version}` : '';
    return this.http.get<OperationsDto>(`${this.base}/api/elections/${roundSlug}/operations${v}`);
  }

  timeline(roundSlug: string): Observable<TimelineDto> {
    return this.http.get<TimelineDto>(`${this.base}/api/elections/${roundSlug}/timeline`);
  }

  timelineAt(roundSlug: string, at: string): Observable<TimelineAtDto> {
    return this.http.get<TimelineAtDto>(
      `${this.base}/api/elections/${roundSlug}/timeline?at=${encodeURIComponent(at)}`,
    );
  }

  compare(roundSlug: string, states: string[], office?: string): Observable<CompareDto> {
    const params = new URLSearchParams({ states: states.join(',') });
    if (office) params.set('office', office);
    return this.http.get<CompareDto>(
      `${this.base}/api/elections/${roundSlug}/compare?${params}`,
    );
  }

  elected(roundSlug: string, office?: string): Observable<ElectedDto> {
    const params = new URLSearchParams();
    if (office) params.set('office', office);
    const q = params.toString() ? `?${params}` : '';
    return this.http.get<ElectedDto>(`${this.base}/api/elections/${roundSlug}/elected${q}`);
  }

  series(roundSlug: string, office: string, area = 'br'): Observable<SeriesDto> {
    const params = new URLSearchParams({ office, area });
    return this.http.get<SeriesDto>(
      `${this.base}/api/elections/${roundSlug}/series?${params}`,
    );
  }

  cities(
    roundSlug: string,
    uf: string,
    opts?: { q?: string; page?: number; pageSize?: number },
  ): Observable<CityPageDto> {
    const params = new URLSearchParams();
    if (opts?.q) params.set('q', opts.q);
    if (opts?.page != null) params.set('page', String(opts.page));
    if (opts?.pageSize != null) params.set('pageSize', String(opts.pageSize));
    const q = params.toString() ? `?${params}` : '';
    return this.http.get<CityPageDto>(
      `${this.base}/api/elections/${roundSlug}/states/${uf}/cities${q}`,
    );
  }

  city(roundSlug: string, uf: string, code: string): Observable<CityDetailDto> {
    return this.http.get<CityDetailDto>(
      `${this.base}/api/elections/${roundSlug}/states/${uf}/cities/${code}`,
    );
  }
}
