package br.dev.bomfim.eleicoes.api.dto;

import java.util.List;

public record StateDetailDto(
    RoundDetailDto round,
    String uf,
    String name,
    ProgressDto progress,
    List<OfficeDto> offices,
    IngestionDto ingestion) {}
