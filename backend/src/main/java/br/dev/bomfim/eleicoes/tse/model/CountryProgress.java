package br.dev.bomfim.eleicoes.tse.model;

import java.util.List;

public record CountryProgress(CountingProgress progress, List<AreaProgressView> states) {}
