package br.dev.bomfim.eleicoes.tse.model;

import java.time.Instant;

/** Progresso de apuração em domínio limpo (sem campos brutos do TSE). */
public record CountingProgress(
    String status,
    Integer sectionsTotal,
    Integer sectionsCounted,
    Double sectionsCountedPct,
    Integer sectionsInstalled,
    Integer sectionsNotInstalled,
    Long electorateTotal,
    Long electorateCounted,
    Long turnout,
    Double turnoutPct,
    Long abstention,
    Double abstentionPct,
    Instant totalizedAt) {}
