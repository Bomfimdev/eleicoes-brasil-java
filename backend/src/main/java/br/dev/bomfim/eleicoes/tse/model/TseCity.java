package br.dev.bomfim.eleicoes.tse.model;

import java.util.List;

public record TseCity(
    String stateCode,
    String providerId,
    String ibgeCode,
    String name,
    boolean capital,
    List<String> zones) {}
