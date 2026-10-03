export type UiPhase = 'aguardando' | 'ao_vivo' | 'encerrado' | 'atrasado' | 'offline';

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
