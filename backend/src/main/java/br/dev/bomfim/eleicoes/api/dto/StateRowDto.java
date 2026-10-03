package br.dev.bomfim.eleicoes.api.dto;

public record StateRowDto(
    String uf,
    String name,
    String region,
    ProgressDto progress,
    String leaderName,
    String leaderParty,
    Double leaderPercent) {}
