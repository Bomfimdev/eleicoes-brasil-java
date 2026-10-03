package br.dev.bomfim.eleicoes.tse.model;

import java.util.List;

public record ElectionConfig(
    String providerRoundId,
    String date,
    int round,
    String progressElectionCode,
    List<String> providerElectionCodes,
    List<TseOffice> offices) {}
