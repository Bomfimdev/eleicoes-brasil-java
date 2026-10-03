package br.dev.bomfim.eleicoes.api.dto;

public record ActivityEventDto(
    String id,
    String type,
    String occurredAt,
    String areaKey,
    String areaName,
    String state,
    Integer sectionsAdded,
    Long votesAdded,
    Double countedPct,
    String message) {}
