package br.dev.bomfim.eleicoes.api.dto;

import java.util.List;

public record TimelineDto(String start, String end, List<TimelinePointDto> points) {

  public record TimelinePointDto(String at, Double countedPct) {}
}
