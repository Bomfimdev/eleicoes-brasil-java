package br.dev.bomfim.eleicoes.api.dto;

import java.util.List;

public record OperationsDto(
    IngestionDto ingestion,
    ProcessingRatesDto processing,
    RequestStatsDto requests,
    DelayDto delay,
    List<FreshnessDto> freshness,
    List<HeatDto> heat,
    List<CycleDto> cycles,
    List<ActivityEventDto> events) {

  public record ProcessingRatesDto(
      double sectionsPerMinute,
      double votesPerMinute,
      double statesPerMinute,
      double citiesPerMinute) {}

  public record RequestStatsDto(
      int windowMinutes,
      int total,
      int ok,
      int notModified,
      int errors,
      Double avgLatencyMs,
      Double p95LatencyMs) {}

  public record DelayDto(Double avgSeconds, Double p95Seconds, int samples) {}

  public record FreshnessDto(
      String areaKey, String name, String updatedAt, String totalizedAt) {}

  public record HeatDto(String uf, int sections, long votes, int updates) {}

  public record CycleDto(
      String id,
      String startedAt,
      Integer durationMs,
      int requests,
      int ok,
      int notModified,
      int errors,
      Double p95LatencyMs,
      String status) {}
}
