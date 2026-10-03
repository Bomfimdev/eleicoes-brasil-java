package br.dev.bomfim.eleicoes.tse.model;

import java.time.Instant;

public record Provenance(
    String provider,
    String adapter,
    String sourceFile,
    String sourceId,
    Instant retrievedAt,
    Instant sourceGeneratedAt,
    String etag,
    String checksum) {}
