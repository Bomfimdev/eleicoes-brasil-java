package br.dev.bomfim.eleicoes.collector;

import br.dev.bomfim.eleicoes.domain.AreaProgress;
import br.dev.bomfim.eleicoes.domain.AreaProgressRepository;
import br.dev.bomfim.eleicoes.domain.AreaResult;
import br.dev.bomfim.eleicoes.domain.AreaResultRepository;
import br.dev.bomfim.eleicoes.domain.Candidate;
import br.dev.bomfim.eleicoes.domain.CandidateRepository;
import br.dev.bomfim.eleicoes.domain.City;
import br.dev.bomfim.eleicoes.domain.CityRepository;
import br.dev.bomfim.eleicoes.domain.CollectorCycle;
import br.dev.bomfim.eleicoes.domain.CollectorCycleRepository;
import br.dev.bomfim.eleicoes.domain.ElectionRound;
import br.dev.bomfim.eleicoes.domain.ElectionRoundRepository;
import br.dev.bomfim.eleicoes.domain.IngestionEvent;
import br.dev.bomfim.eleicoes.domain.IngestionEventRepository;
import br.dev.bomfim.eleicoes.domain.Office;
import br.dev.bomfim.eleicoes.domain.Party;
import br.dev.bomfim.eleicoes.domain.PartyId;
import br.dev.bomfim.eleicoes.domain.PartyRepository;
import br.dev.bomfim.eleicoes.domain.ProgressSnapshot;
import br.dev.bomfim.eleicoes.domain.ProgressSnapshotRepository;
import br.dev.bomfim.eleicoes.domain.ResultSnapshot;
import br.dev.bomfim.eleicoes.domain.ResultSnapshotRepository;
import br.dev.bomfim.eleicoes.tse.model.AreaProgressView;
import br.dev.bomfim.eleicoes.tse.model.AreaResultView;
import br.dev.bomfim.eleicoes.tse.model.CandidateResult;
import br.dev.bomfim.eleicoes.tse.model.CountingProgress;
import br.dev.bomfim.eleicoes.tse.model.PartyResult;
import br.dev.bomfim.eleicoes.tse.model.Provenance;
import br.dev.bomfim.eleicoes.tse.model.TseCity;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CollectorStore {

  private final ElectionRoundRepository roundRepository;
  private final AreaProgressRepository progressRepository;
  private final AreaResultRepository resultRepository;
  private final ProgressSnapshotRepository progressSnapshotRepository;
  private final ResultSnapshotRepository resultSnapshotRepository;
  private final IngestionEventRepository ingestionEventRepository;
  private final CollectorCycleRepository cycleRepository;
  private final CandidateRepository candidateRepository;
  private final PartyRepository partyRepository;
  private final CityRepository cityRepository;
  private final JsonMapper mapper;

  public CollectorStore(
      ElectionRoundRepository roundRepository,
      AreaProgressRepository progressRepository,
      AreaResultRepository resultRepository,
      ProgressSnapshotRepository progressSnapshotRepository,
      ResultSnapshotRepository resultSnapshotRepository,
      IngestionEventRepository ingestionEventRepository,
      CollectorCycleRepository cycleRepository,
      CandidateRepository candidateRepository,
      PartyRepository partyRepository,
      CityRepository cityRepository,
      JsonMapper mapper) {
    this.roundRepository = roundRepository;
    this.progressRepository = progressRepository;
    this.resultRepository = resultRepository;
    this.progressSnapshotRepository = progressSnapshotRepository;
    this.resultSnapshotRepository = resultSnapshotRepository;
    this.ingestionEventRepository = ingestionEventRepository;
    this.cycleRepository = cycleRepository;
    this.candidateRepository = candidateRepository;
    this.partyRepository = partyRepository;
    this.cityRepository = cityRepository;
    this.mapper = mapper;
  }

  public Optional<ElectionRound> findRound(String slug) {
    return roundRepository.findBySlug(slug);
  }

  @Transactional
  public CollectorCycle startCycle(UUID roundId, String mode) {
    CollectorCycle cycle = new CollectorCycle();
    cycle.setId(UUID.randomUUID());
    cycle.setRoundId(roundId);
    cycle.setMode(mode);
    cycle.setStartedAt(Instant.now());
    cycle.setRequests(0);
    cycle.setOk(0);
    cycle.setNotModified(0);
    cycle.setNotFound(0);
    cycle.setErrors(0);
    cycle.setStatus("running");
    return cycleRepository.save(cycle);
  }

  @Transactional
  public void finishCycle(CollectorCycle cycle, String status, String error, CycleStats stats) {
    cycle.setFinishedAt(Instant.now());
    cycle.setStatus(status);
    cycle.setError(error);
    cycle.setRequests(stats.requests);
    cycle.setOk(stats.ok);
    cycle.setNotModified(stats.notModified);
    cycle.setNotFound(stats.notFound);
    cycle.setErrors(stats.errors);
    cycle.setAvgLatencyMs(stats.avgLatency());
    cycle.setP95LatencyMs(stats.p95Latency());
    cycleRepository.save(cycle);
  }

  @Transactional
  public int applyProgress(
      UUID roundId, List<AreaProgressView> entries, Provenance provenance, Instant now) {
    int changed = 0;
    for (AreaProgressView entry : entries) {
      CountingProgress p = entry.progress();
      Optional<AreaProgress> existing =
          progressRepository.findByRoundIdAndAreaKey(roundId, entry.areaKey());
      String progressJson = toJson(p);
      if (existing.isPresent() && sameProgress(existing.get(), p, progressJson)) {
        continue;
      }
      AreaProgress row = existing.orElseGet(AreaProgress::new);
      Integer prevSections = existing.map(this::sectionsOf).orElse(null);
      Long prevTurnout = existing.map(AreaProgress::getTurnout).orElse(null);
      row.setRoundId(roundId);
      row.setAreaKey(entry.areaKey());
      row.setAreaType(entry.areaType());
      row.setStateCode(entry.stateCode());
      row.setStatus(p.status());
      row.setCountedPct(p.sectionsCountedPct());
      row.setTurnout(p.turnout());
      row.setTotalizedAt(p.totalizedAt());
      row.setProgress(progressJson);
      row.setUpdatedAt(now);
      progressRepository.save(row);

      ProgressSnapshot snap = new ProgressSnapshot();
      snap.setRoundId(roundId);
      snap.setAreaKey(entry.areaKey());
      snap.setAreaType(entry.areaType());
      snap.setStateCode(entry.stateCode());
      snap.setCapturedAt(now);
      snap.setTotalizedAt(p.totalizedAt());
      snap.setCountedPct(p.sectionsCountedPct());
      snap.setSectionsCounted(p.sectionsCounted());
      snap.setTurnout(p.turnout());
      snap.setProgress(progressJson);
      snap.setSourceFile(provenance == null ? null : provenance.sourceFile());
      snap.setSourceId(provenance == null ? null : provenance.sourceId());
      progressSnapshotRepository.save(snap);

      IngestionEvent event = new IngestionEvent();
      event.setRoundId(roundId);
      event.setOccurredAt(now);
      event.setType("country".equals(entry.areaType()) ? "progress.country" : "progress.state");
      event.setAreaKey(entry.areaKey());
      event.setStateCode(entry.stateCode());
      event.setSectionsAdded(deltaInt(prevSections, p.sectionsCounted()));
      event.setVotesAdded(deltaLong(prevTurnout, p.turnout()));
      event.setCountedPct(p.sectionsCountedPct());
      ingestionEventRepository.save(event);
      changed++;
    }
    return changed;
  }

  @Transactional
  public boolean applyResult(
      UUID roundId, Office office, AreaResultView result, Provenance provenance, Instant now) {
    Optional<AreaResult> existing =
        resultRepository.findByRoundIdAndOfficeIdAndAreaKey(roundId, office.getId(), result.areaKey());
    String checksum = provenance.checksum() == null ? "" : provenance.checksum();
    if (existing.isPresent() && checksum.equals(existing.get().getChecksum())) {
      return false;
    }

    String previousCandidates = existing.map(AreaResult::getPreviousCandidates).orElse(null);
    if (existing.isPresent() && existing.get().getResult() != null) {
      previousCandidates = compactCandidates(existing.get().getResult());
    }

    AreaResult row = existing.orElseGet(AreaResult::new);
    row.setRoundId(roundId);
    row.setOfficeId(office.getId());
    row.setAreaKey(result.areaKey());
    row.setAreaType(result.areaType());
    row.setStateCode(result.stateCode());
    row.setCountedPct(result.progress().sectionsCountedPct());
    row.setTotalizedAt(result.progress().totalizedAt());
    row.setResult(toJson(result));
    row.setPreviousCandidates(previousCandidates);
    row.setProvenance(toJson(provenance));
    row.setChecksum(checksum);
    row.setUpdatedAt(now);
    resultRepository.save(row);

    ResultSnapshot snap = new ResultSnapshot();
    snap.setRoundId(roundId);
    snap.setOfficeId(office.getId());
    snap.setAreaKey(result.areaKey());
    snap.setAreaType(result.areaType());
    snap.setStateCode(result.stateCode());
    snap.setCapturedAt(now);
    snap.setTotalizedAt(result.progress().totalizedAt());
    snap.setCountedPct(result.progress().sectionsCountedPct());
    snap.setVotes(toJson(result.votes()));
    snap.setCandidates(toJson(result.candidates()));
    snap.setProvenance(toJson(provenance));
    resultSnapshotRepository.save(snap);

    upsertPartiesAndCandidates(roundId, office, result);
    return true;
  }

  @Transactional
  public void setRoundStatus(ElectionRound round, String status) {
    round.setStatus(status);
    round.setUpdatedAt(Instant.now());
    roundRepository.save(round);
  }

  private void upsertPartiesAndCandidates(UUID roundId, Office office, AreaResultView result) {
    for (PartyResult p : result.parties()) {
      if (partyRepository.findById(new PartyId(roundId, p.number())).isPresent()) {
        continue;
      }
      Party party = new Party();
      party.setRoundId(roundId);
      party.setNumber(p.number());
      party.setAbbreviation(p.abbreviation() == null ? "" : p.abbreviation());
      party.setName(p.name() == null ? "" : p.name());
      partyRepository.save(party);
    }
    var known =
        candidateRepository.findByRoundIdAndOfficeIdOrderByNumberAsc(roundId, office.getId()).stream()
            .map(Candidate::getProviderId)
            .collect(java.util.stream.Collectors.toSet());
    for (CandidateResult c : result.candidates()) {
      if (known.contains(c.key())) {
        continue;
      }
      Candidate cand = new Candidate();
      cand.setId(UUID.randomUUID());
      cand.setRoundId(roundId);
      cand.setOfficeId(office.getId());
      cand.setStateCode(result.stateCode());
      cand.setProvider("TSE");
      cand.setProviderId(c.key());
      cand.setNumber(c.number());
      cand.setName(c.name() == null ? c.number() : c.name());
      cand.setBallotName(c.ballotName() == null ? c.name() : c.ballotName());
      cand.setSearchName(
          (c.name() == null ? c.number() : c.name()).toLowerCase(Locale.ROOT));
      cand.setPartyNumber(c.partyNumber() == null ? "" : c.partyNumber());
      cand.setPartyAbbreviation(c.partyAbbreviation() == null ? "" : c.partyAbbreviation());
      cand.setCoalition(c.coalition());
      cand.setRunningMates("[]");
      candidateRepository.save(cand);
      known.add(c.key());
    }
  }

  private boolean sameProgress(AreaProgress row, CountingProgress p, String progressJson) {
    return row.getStatus().equals(p.status())
        && java.util.Objects.equals(row.getCountedPct(), p.sectionsCountedPct())
        && java.util.Objects.equals(row.getTurnout(), p.turnout())
        && java.util.Objects.equals(row.getTotalizedAt(), p.totalizedAt())
        && progressJson.equals(row.getProgress());
  }

  private Integer sectionsOf(AreaProgress row) {
    try {
      return mapper.readTree(row.getProgress()).path("sectionsCounted").isNull()
          ? null
          : mapper.readTree(row.getProgress()).path("sectionsCounted").asInt();
    } catch (Exception e) {
      return null;
    }
  }

  private Integer deltaInt(Integer prev, Integer next) {
    if (prev == null || next == null) {
      return null;
    }
    return next - prev;
  }

  private Long deltaLong(Long prev, Long next) {
    if (prev == null || next == null) {
      return null;
    }
    return next - prev;
  }

  private String compactCandidates(String resultJson) {
    try {
      var node = mapper.readTree(resultJson).path("candidates");
      if (!node.isArray()) {
        return null;
      }
      List<Object[]> compact = new ArrayList<>();
      for (var c : node) {
        compact.add(
            new Object[] {
              c.path("key").asText(),
              c.path("votes").isNull() ? null : c.path("votes").asLong(),
              c.path("percent").isNull() ? null : c.path("percent").asDouble()
            });
      }
      return mapper.writeValueAsString(compact);
    } catch (Exception e) {
      return null;
    }
  }

  /** Upsert de municípios/países do Exterior vindos do mun-cm. */
  @Transactional
  public int upsertCities(List<TseCity> cities) {
    int n = 0;
    for (TseCity src : cities) {
      Optional<City> existing =
          cityRepository.findByProviderAndStateCodeAndProviderId(
              "TSE", src.stateCode(), src.providerId());
      City row = existing.orElseGet(City::new);
      if (row.getId() == null) {
        row.setId(UUID.randomUUID());
      }
      row.setProvider("TSE");
      row.setStateCode(src.stateCode());
      row.setProviderId(src.providerId());
      row.setIbgeCode(src.ibgeCode());
      row.setName(src.name());
      row.setSearchName(src.name().toLowerCase(Locale.ROOT));
      row.setCapital(src.capital());
      row.setZones(src.zones() == null ? new String[0] : src.zones().toArray(String[]::new));
      cityRepository.save(row);
      n++;
    }
    return n;
  }

  private String toJson(Object value) {
    try {
      return mapper.writeValueAsString(value);
    } catch (JacksonException e) {
      throw new IllegalStateException(e);
    }
  }

  public static class CycleStats {
    int requests;
    int ok;
    int notModified;
    int notFound;
    int errors;
    final List<Long> latencies = new ArrayList<>();

    void record(long durationMs, String outcome) {
      requests++;
      latencies.add(durationMs);
      switch (outcome) {
        case "ok" -> ok++;
        case "not-modified" -> notModified++;
        case "not-found" -> notFound++;
        default -> errors++;
      }
    }

    Double avgLatency() {
      if (latencies.isEmpty()) {
        return null;
      }
      return latencies.stream().mapToLong(Long::longValue).average().orElse(0);
    }

    Double p95Latency() {
      if (latencies.isEmpty()) {
        return null;
      }
      List<Long> sorted = new ArrayList<>(latencies);
      sorted.sort(Long::compareTo);
      int idx = (int) Math.ceil(sorted.size() * 0.95) - 1;
      return (double) sorted.get(Math.max(0, idx));
    }
  }
}
