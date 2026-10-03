package br.dev.bomfim.eleicoes.api.dto;

import java.util.List;
import java.util.Map;

public record ResultDto(
    String officeSlug,
    String officeName,
    String areaKey,
    String areaType,
    String stateCode,
    String label,
    Double countedPct,
    String totalizedAt,
    String updatedAt,
    Map<String, Object> votes,
    List<CandidateDto> candidates,
    Object provenance) {}
