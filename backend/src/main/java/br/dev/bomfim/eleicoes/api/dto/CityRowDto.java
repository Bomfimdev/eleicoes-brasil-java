package br.dev.bomfim.eleicoes.api.dto;

public record CityRowDto(
    String code,
    String name,
    boolean capital,
    String ibgeCode,
    ProgressDto progress,
    String leaderName,
    Double leaderPercent) {}
