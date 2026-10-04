package br.dev.bomfim.eleicoes.api.dto;

import java.util.List;

public record StateRowDto(
    String uf,
    String name,
    String region,
    ProgressDto progress,
    String leaderName,
    String leaderParty,
    Double leaderPercent,
    List<StateLeaderDto> presidentTop,
    List<StateLeaderDto> governorTop) {}
