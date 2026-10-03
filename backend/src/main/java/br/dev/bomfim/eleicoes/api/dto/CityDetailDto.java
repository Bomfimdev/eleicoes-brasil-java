package br.dev.bomfim.eleicoes.api.dto;

import java.util.List;

public record CityDetailDto(
    RoundDetailDto round,
    String uf,
    String stateName,
    CityInfo city,
    ProgressDto progress,
    List<ResultDto> results) {

  public record CityInfo(
      String code, String name, boolean capital, String ibgeCode, List<String> zones) {}
}
