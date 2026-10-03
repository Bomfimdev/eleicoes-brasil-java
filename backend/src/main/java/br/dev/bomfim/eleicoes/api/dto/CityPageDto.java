package br.dev.bomfim.eleicoes.api.dto;

import java.util.List;

public record CityPageDto(List<CityRowDto> items, int page, int pageSize, int total) {}
