package br.dev.bomfim.eleicoes.api.dto;

import java.util.List;

public record TimelineAtDto(
    String at, ProgressDto progress, ResultDto headline, List<TimelineStateAtDto> states) {

  public record TimelineStateAtDto(
      String uf, Double countedPct, String leaderName, String leaderParty, Double leaderPercent) {}
}
