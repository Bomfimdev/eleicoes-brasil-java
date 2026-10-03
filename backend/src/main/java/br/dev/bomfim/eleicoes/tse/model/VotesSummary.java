package br.dev.bomfim.eleicoes.tse.model;

public record VotesSummary(
    Long total,
    Long valid,
    Long nominal,
    Long legend,
    Long blank,
    Long nullVotes,
    Long annulled,
    Long annulledSubJudice) {}
