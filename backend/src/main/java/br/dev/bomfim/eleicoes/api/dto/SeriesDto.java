package br.dev.bomfim.eleicoes.api.dto;

import java.util.List;
import java.util.Map;

public record SeriesDto(
    OfficeDto office, String areaKey, List<SeriesCandidateDto> candidates, List<SeriesPointDto> points) {

  public record SeriesCandidateDto(String key, String name, String party, String color) {}

  public record SeriesPointDto(String at, Double countedPct, Map<String, Double> values) {}
}
