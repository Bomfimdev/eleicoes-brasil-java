package br.dev.bomfim.eleicoes.api.dto;

import java.util.List;

public record RoundDetailDto(
    String slug,
    String electionSlug,
    String electionName,
    int year,
    String kind,
    int round,
    String date,
    String status,
    String environment,
    boolean demo,
    String adapter,
    List<OfficeDto> offices) {}
