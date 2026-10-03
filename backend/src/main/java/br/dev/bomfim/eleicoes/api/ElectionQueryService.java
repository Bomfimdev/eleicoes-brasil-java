package br.dev.bomfim.eleicoes.api;

import br.dev.bomfim.eleicoes.api.dto.CandidateDto;
import br.dev.bomfim.eleicoes.api.dto.ElectionSummaryDto;
import br.dev.bomfim.eleicoes.api.dto.IngestionDto;
import br.dev.bomfim.eleicoes.api.dto.OfficeDto;
import br.dev.bomfim.eleicoes.api.dto.OverviewDto;
import br.dev.bomfim.eleicoes.api.dto.ProgressDto;
import br.dev.bomfim.eleicoes.api.dto.ResultDto;
import br.dev.bomfim.eleicoes.api.dto.RoundDetailDto;
import br.dev.bomfim.eleicoes.api.dto.RoundSummaryDto;
import br.dev.bomfim.eleicoes.api.dto.StateDetailDto;
import br.dev.bomfim.eleicoes.api.dto.StateRowDto;
import br.dev.bomfim.eleicoes.domain.AreaProgress;
import br.dev.bomfim.eleicoes.domain.AreaProgressRepository;
import br.dev.bomfim.eleicoes.domain.AreaResult;
import br.dev.bomfim.eleicoes.domain.AreaResultRepository;
import br.dev.bomfim.eleicoes.domain.CollectorCycle;
import br.dev.bomfim.eleicoes.domain.CollectorCycleRepository;
import br.dev.bomfim.eleicoes.domain.Election;
import br.dev.bomfim.eleicoes.domain.ElectionRepository;
import br.dev.bomfim.eleicoes.domain.ElectionRound;
import br.dev.bomfim.eleicoes.domain.ElectionRoundRepository;
import br.dev.bomfim.eleicoes.domain.Office;
import br.dev.bomfim.eleicoes.domain.OfficeRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ElectionQueryService {

  private final ElectionRepository electionRepository;
  private final ElectionRoundRepository roundRepository;
  private final OfficeRepository officeRepository;
  private final AreaProgressRepository progressRepository;
  private final AreaResultRepository resultRepository;
  private final CollectorCycleRepository cycleRepository;
  private final JsonMapper mapper;

  public ElectionQueryService(
      ElectionRepository electionRepository,
      ElectionRoundRepository roundRepository,
      OfficeRepository officeRepository,
      AreaProgressRepository progressRepository,
      AreaResultRepository resultRepository,
      CollectorCycleRepository cycleRepository,
      JsonMapper mapper) {
    this.electionRepository = electionRepository;
    this.roundRepository = roundRepository;
    this.officeRepository = officeRepository;
    this.progressRepository = progressRepository;
    this.resultRepository = resultRepository;
    this.cycleRepository = cycleRepository;
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public List<ElectionSummaryDto> listElections() {
    List<Election> elections =
        electionRepository.findAll().stream()
            .sorted(
                Comparator.comparing(Election::isDemo)
                    .thenComparing(Election::getYear, Comparator.reverseOrder())
                    .thenComparing(Election::getSlug))
            .toList();
    List<ElectionSummaryDto> out = new ArrayList<>();
    for (Election e : elections) {
      List<RoundSummaryDto> rounds =
          roundRepository.findByElectionIdOrderByRoundAsc(e.getId()).stream()
              .map(r -> toSummary(r, e))
              .toList();
      out.add(
          new ElectionSummaryDto(
              e.getSlug(), e.getName(), e.getYear(), e.getKind(), e.isDemo(), rounds));
    }
    return out;
  }

  @Transactional(readOnly = true)
  public RoundDetailDto getRound(String slug) {
    return toDetail(requireRound(slug));
  }

  @Transactional(readOnly = true)
  public OverviewDto overview(String slug) {
    LoadedRound loaded = requireRound(slug);
    RoundDetailDto round = toDetail(loaded);
    ProgressDto progress = progressOf(loaded.round().getId(), "br");
    IngestionDto ingestion = ingestion(loaded.round());
    Office headlineOffice =
        loaded.offices().stream()
            .filter(o -> "country".equals(o.getScope()))
            .findFirst()
            .orElse(null);
    ResultDto headline = null;
    if (headlineOffice != null) {
      headline =
          resultRepository
              .findByRoundIdAndOfficeIdAndAreaKey(loaded.round().getId(), headlineOffice.getId(), "br")
              .map(row -> toResult(headlineOffice, row, "Brasil"))
              .orElse(null);
    }
    List<StateRowDto> states = stateRows(loaded, headlineOffice);
    return new OverviewDto(round, progress, ingestion, headline, states);
  }

  @Transactional(readOnly = true)
  public ResultDto result(String slug, String officeSlug, String areaKey) {
    LoadedRound loaded = requireRound(slug);
    String area = (areaKey == null || areaKey.isBlank()) ? "br" : areaKey.toLowerCase(Locale.ROOT);
    Office office = resolveOffice(loaded, officeSlug);
    return resultRepository
        .findByRoundIdAndOfficeIdAndAreaKey(loaded.round().getId(), office.getId(), area)
        .map(row -> toResult(office, row, labelFor(area)))
        .orElseThrow(() -> new ApiNotFoundException("sem resultados para este cargo e área"));
  }

  @Transactional(readOnly = true)
  public List<StateRowDto> states(String slug) {
    LoadedRound loaded = requireRound(slug);
    Office headline =
        loaded.offices().stream()
            .filter(o -> "country".equals(o.getScope()))
            .findFirst()
            .orElse(null);
    return stateRows(loaded, headline);
  }

  @Transactional(readOnly = true)
  public StateDetailDto state(String slug, String uf) {
    BrazilianStates.State meta = BrazilianStates.require(uf);
    LoadedRound loaded = requireRound(slug);
    List<OfficeDto> offices =
        loaded.offices().stream()
            .filter(
                o ->
                    "state".equals(o.getScope())
                        && (o.getStates() == null
                            || o.getStates().length == 0
                            || List.of(o.getStates()).contains(meta.code())))
            .map(this::toOffice)
            .toList();
    return new StateDetailDto(
        toDetail(loaded),
        meta.code(),
        meta.name(),
        progressOf(loaded.round().getId(), meta.code().toLowerCase(Locale.ROOT)),
        offices,
        ingestion(loaded.round()));
  }

  private List<StateRowDto> stateRows(LoadedRound loaded, Office headlineOffice) {
    Map<String, AreaProgress> byUf =
        progressRepository.findByRoundIdAndAreaType(loaded.round().getId(), "state").stream()
            .collect(
                Collectors.toMap(
                    p -> p.getAreaKey().toUpperCase(Locale.ROOT), p -> p, (a, b) -> a));

    Map<String, CandidateDto> leaders = new LinkedHashMap<>();
    if (headlineOffice != null) {
      List<AreaResult> stateResults =
          resultRepository.findByRoundIdAndOfficeIdAndAreaType(
              loaded.round().getId(), headlineOffice.getId(), "state");
      for (AreaResult row : stateResults) {
        CandidateDto top = topCandidate(row);
        if (top != null && row.getStateCode() != null) {
          leaders.put(row.getStateCode().toUpperCase(Locale.ROOT), top);
        }
      }
    }

    List<StateRowDto> out = new ArrayList<>();
    for (BrazilianStates.State s : BrazilianStates.ALL) {
      AreaProgress p = byUf.get(s.code());
      CandidateDto leader = leaders.get(s.code());
      out.add(
          new StateRowDto(
              s.code(),
              s.name(),
              s.region(),
              p == null ? null : toProgress(p),
              leader == null ? null : leader.ballotName(),
              leader == null ? null : leader.partyAbbreviation(),
              leader == null ? null : leader.percent()));
    }
    return out;
  }

  private Office resolveOffice(LoadedRound loaded, String officeSlug) {
    if (officeSlug == null || officeSlug.isBlank()) {
      return loaded.offices().stream()
          .filter(o -> "presidente".equals(o.getSlug()) || "country".equals(o.getScope()))
          .findFirst()
          .orElseThrow(() -> new ApiNotFoundException("cargo headline não encontrado"));
    }
    return loaded.offices().stream()
        .filter(o -> officeSlug.equals(o.getSlug()) || officeSlug.equals(o.getProviderId()))
        .findFirst()
        .orElseThrow(() -> new ApiNotFoundException("cargo não encontrado: " + officeSlug));
  }

  private LoadedRound requireRound(String slug) {
    ElectionRound round =
        roundRepository
            .findBySlug(slug)
            .orElseThrow(() -> new ApiNotFoundException("turno não encontrado: " + slug));
    Election election =
        electionRepository
            .findById(round.getElectionId())
            .orElseThrow(() -> new ApiNotFoundException("eleição não encontrada"));
    List<Office> offices = officeRepository.findByRoundIdOrderBySlugAsc(round.getId());
    return new LoadedRound(round, election, offices);
  }

  private RoundSummaryDto toSummary(ElectionRound r, Election e) {
    String adapter =
        r.getAdapter() != null && r.getAdapterVersion() != null
            ? r.getAdapter() + "@" + r.getAdapterVersion()
            : r.getAdapter();
    return new RoundSummaryDto(
        r.getSlug(),
        e.getSlug(),
        e.getName(),
        e.getYear(),
        e.getKind(),
        r.getRound(),
        r.getDate() == null ? null : r.getDate().toString(),
        r.getStatus(),
        r.getEnvironment(),
        e.isDemo(),
        adapter);
  }

  private RoundDetailDto toDetail(LoadedRound loaded) {
    RoundSummaryDto s = toSummary(loaded.round(), loaded.election());
    return new RoundDetailDto(
        s.slug(),
        s.electionSlug(),
        s.electionName(),
        s.year(),
        s.kind(),
        s.round(),
        s.date(),
        s.status(),
        s.environment(),
        s.demo(),
        s.adapter(),
        loaded.offices().stream().map(this::toOffice).toList());
  }

  private OfficeDto toOffice(Office o) {
    List<String> states = o.getStates() == null ? null : List.of(o.getStates());
    return new OfficeDto(o.getProviderId(), o.getSlug(), o.getName(), o.getKind(), o.getScope(), states);
  }

  private ProgressDto progressOf(UUID roundId, String areaKey) {
    return progressRepository
        .findByRoundIdAndAreaKey(roundId, areaKey)
        .map(this::toProgress)
        .orElse(null);
  }

  private ProgressDto toProgress(AreaProgress p) {
    Object details = parseJson(p.getProgress());
    return new ProgressDto(
        p.getStatus(),
        p.getCountedPct(),
        p.getTurnout(),
        iso(p.getTotalizedAt()),
        iso(p.getUpdatedAt()),
        details);
  }

  private IngestionDto ingestion(ElectionRound round) {
    Optional<CollectorCycle> last =
        cycleRepository.findFirstByRoundIdAndStatusNotOrderByStartedAtDesc(round.getId(), "running");
    if (last.isEmpty()) {
      return new IngestionDto("idle", null, null, null, null);
    }
    CollectorCycle c = last.get();
    Optional<CollectorCycle> ok =
        cycleRepository.findFirstByRoundIdAndStatusInOrderByStartedAtDesc(
            round.getId(), List.of("ok", "degraded"));
    long ageMs = Instant.now().toEpochMilli() - c.getStartedAt().toEpochMilli();
    String state;
    if (ageMs > ("waiting".equals(c.getStatus()) ? 180_000 : 120_000)) {
      state = "final".equals(round.getStatus()) || "REPLAY".equals(c.getMode()) ? "idle" : "offline";
    } else if ("waiting".equals(c.getStatus())) {
      state = "waiting";
    } else if ("ok".equals(c.getStatus())) {
      state = "healthy";
    } else {
      state = "degraded";
    }
    Instant successAt = ok.map(x -> x.getFinishedAt() != null ? x.getFinishedAt() : x.getStartedAt()).orElse(null);
    return new IngestionDto(state, c.getMode(), iso(c.getStartedAt()), iso(successAt), c.getError());
  }

  private ResultDto toResult(Office office, AreaResult row, String label) {
    JsonNode root = parseTree(row.getResult());
    Map<String, Object> votes = new LinkedHashMap<>();
    JsonNode votesNode = root.path("votes");
    if (votesNode.isObject()) {
      votesNode
          .propertyNames()
          .forEach(
              name -> {
                JsonNode v = votesNode.get(name);
                votes.put(name, v == null || v.isNull() ? null : mapper.treeToValue(v, Object.class));
              });
    }
    List<CandidateDto> candidates = new ArrayList<>();
    JsonNode candNode = root.path("candidates");
    if (candNode.isArray()) {
      for (JsonNode c : candNode) {
        candidates.add(
            new CandidateDto(
                text(c, "key"),
                text(c, "number"),
                text(c, "name"),
                text(c, "ballotName"),
                text(c, "partyNumber"),
                text(c, "partyAbbreviation"),
                longOrNull(c, "votes"),
                doubleOrNull(c, "percent"),
                boolOrNull(c, "elected")));
      }
    }
    candidates.sort(
        Comparator.comparing((CandidateDto c) -> c.votes() == null ? Long.MIN_VALUE : c.votes())
            .reversed());
    return new ResultDto(
        office.getSlug(),
        office.getName(),
        row.getAreaKey(),
        row.getAreaType(),
        row.getStateCode(),
        label,
        row.getCountedPct(),
        iso(row.getTotalizedAt()),
        iso(row.getUpdatedAt()),
        votes,
        candidates,
        parseJson(row.getProvenance()));
  }

  private CandidateDto topCandidate(AreaResult row) {
    JsonNode candNode = parseTree(row.getResult()).path("candidates");
    CandidateDto best = null;
    long bestVotes = Long.MIN_VALUE;
    if (candNode.isArray()) {
      for (JsonNode c : candNode) {
        Long votes = longOrNull(c, "votes");
        long v = votes == null ? Long.MIN_VALUE : votes;
        if (v > bestVotes) {
          bestVotes = v;
          best =
              new CandidateDto(
                  text(c, "key"),
                  text(c, "number"),
                  text(c, "name"),
                  text(c, "ballotName"),
                  text(c, "partyNumber"),
                  text(c, "partyAbbreviation"),
                  votes,
                  doubleOrNull(c, "percent"),
                  boolOrNull(c, "elected"));
        }
      }
    }
    return bestVotes == Long.MIN_VALUE ? null : best;
  }

  private static String labelFor(String area) {
    if ("br".equalsIgnoreCase(area)) {
      return "Brasil";
    }
    if (BrazilianStates.isValid(area)) {
      return BrazilianStates.require(area).name();
    }
    return area;
  }

  private Object parseJson(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      return mapper.readValue(raw, Object.class);
    } catch (Exception e) {
      return raw;
    }
  }

  private JsonNode parseTree(String raw) {
    if (raw == null || raw.isBlank()) {
      return mapper.createObjectNode();
    }
    try {
      return mapper.readTree(raw);
    } catch (Exception e) {
      return mapper.createObjectNode();
    }
  }

  private static String iso(Instant instant) {
    return instant == null ? null : instant.toString();
  }

  private static String text(JsonNode n, String field) {
    JsonNode v = n.path(field);
    return v.isMissingNode() || v.isNull() ? null : v.asText();
  }

  private static Long longOrNull(JsonNode n, String field) {
    JsonNode v = n.path(field);
    if (v.isMissingNode() || v.isNull()) {
      return null;
    }
    return v.asLong();
  }

  private static Double doubleOrNull(JsonNode n, String field) {
    JsonNode v = n.path(field);
    if (v.isMissingNode() || v.isNull()) {
      return null;
    }
    return v.asDouble();
  }

  private static Boolean boolOrNull(JsonNode n, String field) {
    JsonNode v = n.path(field);
    if (v.isMissingNode() || v.isNull()) {
      return null;
    }
    return v.asBoolean();
  }

  private record LoadedRound(ElectionRound round, Election election, List<Office> offices) {}
}
