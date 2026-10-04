package br.dev.bomfim.eleicoes.tse.model;

public record PartyResult(
    String number,
    String abbreviation,
    String name,
    Long nominalVotes,
    Long legendVotes,
    /** Vagas do partido/coligação no arquivo TSE (`vag`), quando houver. */
    Integer seats) {}
