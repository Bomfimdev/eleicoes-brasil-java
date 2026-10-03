package br.dev.bomfim.eleicoes.tse.model;

import java.util.List;

public record AreaResultView(
    String officeCode,
    String areaKey,
    String areaType,
    String stateCode,
    CountingProgress progress,
    VotesSummary votes,
    List<CandidateResult> candidates,
    List<PartyResult> parties,
    Integer seats,
    boolean finalResult,
    Boolean votesPublishable) {}
