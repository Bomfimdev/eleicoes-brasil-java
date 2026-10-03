package br.dev.bomfim.eleicoes.tse.model;

public record CandidateResult(
    String key,
    String number,
    String name,
    String ballotName,
    String partyNumber,
    String partyAbbreviation,
    String partyName,
    String coalition,
    Long votes,
    Double percent,
    Boolean elected,
    String status,
    String voteDestination) {}
