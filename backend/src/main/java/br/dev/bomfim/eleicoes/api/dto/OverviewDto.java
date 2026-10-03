package br.dev.bomfim.eleicoes.api.dto;

import java.util.List;

public record OverviewDto(
    RoundDetailDto round,
    ProgressDto progress,
    IngestionDto ingestion,
    ResultDto headline,
    List<StateRowDto> states) {}
