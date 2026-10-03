package br.dev.bomfim.eleicoes.api.dto;

import java.util.List;

public record OfficeDto(
    String code, String slug, String name, String kind, String scope, List<String> states) {}
