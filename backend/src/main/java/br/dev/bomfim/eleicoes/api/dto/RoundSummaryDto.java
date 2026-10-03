package br.dev.bomfim.eleicoes.api.dto;

public record RoundSummaryDto(
    String slug,
    String electionSlug,
    String electionName,
    int year,
    String kind,
    int round,
    String date,
    String status,
    String environment,
    boolean demo,
    String adapter) {}
