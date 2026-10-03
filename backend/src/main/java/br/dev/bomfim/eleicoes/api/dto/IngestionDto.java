package br.dev.bomfim.eleicoes.api.dto;

public record IngestionDto(
    String state, String mode, String lastCycleAt, String lastSuccessAt, String lastError) {}
