package br.dev.bomfim.eleicoes.tse.model;

public record TseOffice(
    String code,
    String slug,
    String name,
    String kind,
    String scope,
    String providerElectionCode) {}
