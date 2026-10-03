package br.dev.bomfim.eleicoes.collector;

import br.dev.bomfim.eleicoes.api.RealtimeVersionHub;
import br.dev.bomfim.eleicoes.config.EleicoesProperties;
import br.dev.bomfim.eleicoes.domain.AreaProgress;
import br.dev.bomfim.eleicoes.domain.AreaProgressRepository;
import br.dev.bomfim.eleicoes.domain.AreaResult;
import br.dev.bomfim.eleicoes.domain.AreaResultRepository;
import br.dev.bomfim.eleicoes.domain.CollectorCycle;
import br.dev.bomfim.eleicoes.domain.ElectionRound;
import br.dev.bomfim.eleicoes.domain.ProgressSnapshot;
import br.dev.bomfim.eleicoes.domain.ProgressSnapshotRepository;
import br.dev.bomfim.eleicoes.domain.ResultSnapshot;
import br.dev.bomfim.eleicoes.domain.ResultSnapshotRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Modo REPLAY: reaplica snapshots já gravados no banco (sem falar com o TSE), na ordem de
 * captura, um instante por ciclo do scheduler.
 */
@Service
public class ReplayService {

  private static final Logger log = LoggerFactory.getLogger(ReplayService.class);

  private final EleicoesProperties properties;
  private final CollectorStore store;
  private final ProgressSnapshotRepository progressSnapshotRepository;
  private final ResultSnapshotRepository resultSnapshotRepository;
  private final AreaProgressRepository progressRepository;
  private final AreaResultRepository resultRepository;
  private final RealtimeVersionHub versionHub;

  private List<Instant> timeline = List.of();
  private int cursor = 0;
  private UUID roundId;
  private String roundSlug;
  private boolean finished;

  public ReplayService(
      EleicoesProperties properties,
      CollectorStore store,
      ProgressSnapshotRepository progressSnapshotRepository,
      ResultSnapshotRepository resultSnapshotRepository,
      AreaProgressRepository progressRepository,
      AreaResultRepository resultRepository,
      RealtimeVersionHub versionHub) {
    this.properties = properties;
    this.store = store;
    this.progressSnapshotRepository = progressSnapshotRepository;
    this.resultSnapshotRepository = resultSnapshotRepository;
    this.progressRepository = progressRepository;
    this.resultRepository = resultRepository;
    this.versionHub = versionHub;
  }

  /** @return status do ciclo: ok, waiting, finished, failed */
  @Transactional
  public synchronized String runTick() {
    if (finished) {
      return "finished";
    }
    String slug = properties.getReplay().getSourceRoundSlug();
    Optional<ElectionRound> roundOpt = store.findRound(slug);
    if (roundOpt.isEmpty()) {
      log.warn("REPLAY: round {} não encontrado", slug);
      return "failed";
    }
    ElectionRound round = roundOpt.get();
    if (timeline.isEmpty() || !round.getId().equals(roundId)) {
      loadTimeline(round);
    }
    if (timeline.isEmpty()) {
      log.warn("REPLAY: nenhum snapshot em {}", slug);
      return "waiting";
    }

    CollectorCycle cycle = store.startCycle(round.getId(), "REPLAY");
    CollectorStore.CycleStats stats = new CollectorStore.CycleStats();
    Instant at = timeline.get(cursor);
    applyAt(round.getId(), at);
    cursor++;
    stats.record(0, "ok");

    if (cursor >= timeline.size()) {
      finished = true;
      store.setRoundStatus(round, "final");
      store.finishCycle(cycle, "ok", null, stats);
      versionHub.publish(round.getSlug());
      log.info("REPLAY concluído para {} ({} passos)", slug, timeline.size());
      return "finished";
    }

    store.setRoundStatus(round, "live");
    store.finishCycle(cycle, "ok", null, stats);
    versionHub.publish(round.getSlug());
    log.info("REPLAY passo {}/{} @ {}", cursor, timeline.size(), at);
    return "ok";
  }

  private void loadTimeline(ElectionRound round) {
    roundId = round.getId();
    roundSlug = round.getSlug();
    cursor = 0;
    finished = false;
    TreeSet<Instant> times = new TreeSet<>();
    for (ProgressSnapshot s : progressSnapshotRepository.findByRoundIdOrderByCapturedAtAscIdAsc(roundId)) {
      if (s.getCapturedAt() != null) {
        times.add(s.getCapturedAt());
      }
    }
    for (ResultSnapshot s : resultSnapshotRepository.findByRoundIdOrderByCapturedAtAscIdAsc(roundId)) {
      if (s.getCapturedAt() != null) {
        times.add(s.getCapturedAt());
      }
    }
    timeline = new ArrayList<>(times);
    log.info("REPLAY carregou {} instantes de snapshot para {}", timeline.size(), roundSlug);
  }

  private void applyAt(UUID rid, Instant at) {
    Instant now = Instant.now();
    List<ProgressSnapshot> progresses =
        progressSnapshotRepository.findByRoundIdOrderByCapturedAtAscIdAsc(rid).stream()
            .filter(s -> at.equals(s.getCapturedAt()))
            .toList();
    for (ProgressSnapshot s : progresses) {
      AreaProgress row =
          progressRepository.findByRoundIdAndAreaKey(rid, s.getAreaKey()).orElseGet(AreaProgress::new);
      row.setRoundId(rid);
      row.setAreaKey(s.getAreaKey());
      row.setAreaType(s.getAreaType());
      row.setStateCode(s.getStateCode());
      row.setStatus(statusFromProgressJson(s.getProgress()));
      row.setCountedPct(s.getCountedPct());
      row.setTurnout(s.getTurnout());
      row.setTotalizedAt(s.getTotalizedAt());
      row.setProgress(s.getProgress());
      row.setUpdatedAt(now);
      progressRepository.save(row);
    }

    List<ResultSnapshot> results =
        resultSnapshotRepository.findByRoundIdOrderByCapturedAtAscIdAsc(rid).stream()
            .filter(s -> at.equals(s.getCapturedAt()))
            .sorted(Comparator.comparing(ResultSnapshot::getId))
            .toList();
    for (ResultSnapshot s : results) {
      AreaResult row =
          resultRepository
              .findByRoundIdAndOfficeIdAndAreaKey(rid, s.getOfficeId(), s.getAreaKey())
              .orElseGet(AreaResult::new);
      row.setRoundId(rid);
      row.setOfficeId(s.getOfficeId());
      row.setAreaKey(s.getAreaKey());
      row.setAreaType(s.getAreaType());
      row.setStateCode(s.getStateCode());
      row.setCountedPct(s.getCountedPct());
      row.setTotalizedAt(s.getTotalizedAt());
      String resultJson =
          "{\"votes\":"
              + nullToEmptyObject(s.getVotes())
              + ",\"candidates\":"
              + nullToEmptyArray(s.getCandidates())
              + "}";
      row.setResult(resultJson);
      row.setProvenance(s.getProvenance() == null ? "{}" : s.getProvenance());
      row.setChecksum("replay-" + s.getId());
      row.setUpdatedAt(now);
      resultRepository.save(row);
    }
  }

  private static String statusFromProgressJson(String json) {
    if (json == null) {
      return "in-progress";
    }
    int i = json.indexOf("\"status\"");
    if (i < 0) {
      return "in-progress";
    }
    int colon = json.indexOf(':', i);
    int q1 = json.indexOf('"', colon + 1);
    int q2 = json.indexOf('"', q1 + 1);
    if (q1 < 0 || q2 < 0) {
      return "in-progress";
    }
    return json.substring(q1 + 1, q2);
  }

  private static String nullToEmptyObject(String json) {
    return json == null || json.isBlank() ? "{}" : json;
  }

  private static String nullToEmptyArray(String json) {
    return json == null || json.isBlank() ? "[]" : json;
  }
}
