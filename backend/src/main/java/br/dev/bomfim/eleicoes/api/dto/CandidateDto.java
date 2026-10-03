package br.dev.bomfim.eleicoes.api.dto;

public record CandidateDto(
    String key,
    String number,
    String name,
    String ballotName,
    String partyNumber,
    String partyAbbreviation,
    Long votes,
    Double percent,
    Boolean elected) {}
