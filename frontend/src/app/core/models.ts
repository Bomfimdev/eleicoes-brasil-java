export type UiPhase = 'aguardando' | 'ao_vivo' | 'encerrado' | 'atrasado' | 'offline';

export interface RoundSummary {
  slug: string;
  electionSlug: string;
  electionName: string;
  year: number;
  kind: string;
  round: number;
  date: string;
  status: string;
  environment: string | null;
  demo: boolean;
  adapter: string | null;
}

export interface ElectionSummary {
  slug: string;
  name: string;
  year: number;
  kind: string;
  demo: boolean;
  rounds: RoundSummary[];
}

export interface RoundDetail {
  slug: string;
  electionSlug: string;
  electionName: string;
  year: number;
  kind: string;
  round: number;
  date: string;
  status: string;
  environment: string | null;
  demo: boolean;
  adapter: string | null;
  offices: OfficeDto[];
}

export interface OfficeDto {
  code: string;
  slug: string;
  name: string;
  kind: string;
  scope: string;
  states: string[] | null;
}

export interface ProgressDto {
  status: string | null;
  countedPct: number | null;
  turnout: number | null;
  totalizedAt: string | null;
  updatedAt: string | null;
  details: unknown;
}

export interface IngestionDto {
  state: string;
  mode: string | null;
  lastCycleAt: string | null;
  lastSuccessAt: string | null;
  lastError: string | null;
}

export interface CandidateDto {
  key: string | null;
  number: string | null;
  name: string | null;
  ballotName: string | null;
  partyNumber: string | null;
  partyAbbreviation: string | null;
  votes: number | null;
  percent: number | null;
  elected: boolean | null;
}

export interface ResultDto {
  officeSlug: string;
  officeName: string;
  areaKey: string;
  areaType: string;
  stateCode: string | null;
  label: string;
  countedPct: number | null;
  totalizedAt: string | null;
  updatedAt: string | null;
  votes: Record<string, number | null>;
  candidates: CandidateDto[];
  provenance: unknown;
}

export interface StateRowDto {
  uf: string;
  name: string;
  region: string;
  progress: ProgressDto | null;
  leaderName: string | null;
  leaderParty: string | null;
  leaderPercent: number | null;
}

export interface OverviewDto {
  round: RoundDetail;
  progress: ProgressDto | null;
  ingestion: IngestionDto;
  headline: ResultDto | null;
  states: StateRowDto[];
}

export interface StateDetailDto {
  round: RoundDetail;
  uf: string;
  name: string;
  progress: ProgressDto | null;
  offices: OfficeDto[];
  ingestion: IngestionDto;
}

export interface ActivityEventDto {
  id: string;
  type: string;
  occurredAt: string;
  areaKey: string | null;
  areaName: string | null;
  state: string | null;
  sectionsAdded: number | null;
  votesAdded: number | null;
  countedPct: number | null;
  message: string | null;
}

export interface OperationsDto {
  ingestion: IngestionDto;
  processing: {
    sectionsPerMinute: number;
    votesPerMinute: number;
    statesPerMinute: number;
    citiesPerMinute: number;
  };
  requests: {
    windowMinutes: number;
    total: number;
    ok: number;
    notModified: number;
    errors: number;
    avgLatencyMs: number | null;
    p95LatencyMs: number | null;
  };
  delay: { avgSeconds: number | null; p95Seconds: number | null; samples: number };
  freshness: { areaKey: string; name: string; updatedAt: string | null; totalizedAt: string | null }[];
  heat: { uf: string; sections: number; votes: number; updates: number }[];
  cycles: {
    id: string;
    startedAt: string;
    durationMs: number | null;
    requests: number;
    ok: number;
    notModified: number;
    errors: number;
    p95LatencyMs: number | null;
    status: string;
  }[];
  events: ActivityEventDto[];
}

export interface TimelineDto {
  start: string | null;
  end: string | null;
  points: { at: string; countedPct: number | null }[];
}

export interface TimelineAtDto {
  at: string;
  progress: ProgressDto | null;
  headline: ResultDto | null;
  states: {
    uf: string;
    countedPct: number | null;
    leaderName: string | null;
    leaderParty: string | null;
    leaderPercent: number | null;
  }[];
}

export interface CompareDto {
  office: OfficeDto | null;
  states: {
    uf: string;
    name: string;
    progress: ProgressDto | null;
    votes: Record<string, number | null> | null;
    candidates: {
      key: string | null;
      name: string | null;
      party: string | null;
      percent: number | null;
      votes: number | null;
    }[];
  }[];
}
