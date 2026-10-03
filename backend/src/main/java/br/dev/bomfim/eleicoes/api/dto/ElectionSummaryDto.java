package br.dev.bomfim.eleicoes.api.dto;

import java.util.List;

public record ElectionSummaryDto(
    String slug, String name, int year, String kind, boolean demo, List<RoundSummaryDto> rounds) {}
