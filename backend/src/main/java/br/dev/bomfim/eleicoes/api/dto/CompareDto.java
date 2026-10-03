package br.dev.bomfim.eleicoes.api.dto;

import java.util.List;
import java.util.Map;

public record CompareDto(OfficeDto office, List<CompareStateDto> states) {

  public record CompareStateDto(
      String uf,
      String name,
      ProgressDto progress,
      Map<String, Object> votes,
      List<CompareCandidateDto> candidates) {}

  public record CompareCandidateDto(
      String key, String name, String party, Double percent, Long votes) {}
}