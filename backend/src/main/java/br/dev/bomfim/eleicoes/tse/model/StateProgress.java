package br.dev.bomfim.eleicoes.tse.model;

import java.util.List;

public record StateProgress(
    String state, CountingProgress progress, List<AreaProgressView> cities) {}
