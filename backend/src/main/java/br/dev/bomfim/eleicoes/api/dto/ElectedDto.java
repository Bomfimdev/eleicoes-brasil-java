package br.dev.bomfim.eleicoes.api.dto;

import java.util.List;

public record ElectedDto(List<OfficeGroupDto> offices) {

  public record OfficeGroupDto(
      String officeSlug, String officeName, int count, List<ElectedPersonDto> people) {}

  public record ElectedPersonDto(
      String uf,
      String stateName,
      String name,
      String party,
      Double percent,
      Long votes,
      String status) {}
}
