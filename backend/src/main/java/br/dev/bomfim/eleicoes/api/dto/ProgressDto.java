package br.dev.bomfim.eleicoes.api.dto;

public record ProgressDto(
    String status,
    Double countedPct,
    Long turnout,
    String totalizedAt,
    String updatedAt,
    Object details) {}
