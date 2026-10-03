package br.dev.bomfim.eleicoes.collector;

import br.dev.bomfim.eleicoes.config.EleicoesProperties;
import br.dev.bomfim.eleicoes.domain.CollectorCycle;
import br.dev.bomfim.eleicoes.domain.ElectionRound;
import br.dev.bomfim.eleicoes.domain.Office;
import br.dev.bomfim.eleicoes.domain.OfficeRepository;
import br.dev.bomfim.eleicoes.tse.TseAdapter2026;
import br.dev.bomfim.eleicoes.tse.TseClient;
import br.dev.bomfim.eleicoes.tse.TsePayloadException;
import br.dev.bomfim.eleicoes.tse.model.AreaProgressView;
import br.dev.bomfim.eleicoes.tse.model.AreaResultView;
import br.dev.bomfim.eleicoes.tse.model.CountryProgress;
import br.dev.bomfim.eleicoes.tse.model.ElectionConfig;
import br.dev.bomfim.eleicoes.tse.model.Fetched;
import br.dev.bomfim.eleicoes.tse.model.TseOffice;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CollectorService {

  private static final Logger log = LoggerFactory.getLogger(CollectorService.class);

  private final EleicoesProperties properties;
  private final ElectionWindowGate gate;
  private final CollectorStore store;
  private final OfficeRepository officeRepository;
  private final TseAdapter2026 adapter;

  public CollectorService(
      EleicoesProperties properties,
      ElectionWindowGate gate,
      CollectorStore store,
      OfficeRepository officeRepository,
      TseAdapter2026 adapter) {
    this.properties = properties;
    this.gate = gate;
    this.store = store;
    this.officeRepository = officeRepository;
    this.adapter = adapter;
  }

  /** @return status do ciclo: skipped, ok, degraded, failed, waiting */
  public String runCycle() {
    if (!properties.getCollector().isEnabled()) {
      return "skipped";
    }
    if (!gate.isOpen()) {
      log.debug("Collector fora da janela eleitoral — no-op");
      return "skipped";
    }

    Optional<ElectionRound> roundOpt = store.findRound(properties.getCollector().getRoundSlug());
    if (roundOpt.isEmpty()) {
      log.warn("Round {} não encontrado — ciclo abortado", properties.getCollector().getRoundSlug());
      return "failed";
    }
    ElectionRound round = roundOpt.get();
    String mode = properties.getAppMode();
    CollectorCycle cycle = store.startCycle(round.getId(), mode);
    CollectorStore.CycleStats stats = new CollectorStore.CycleStats();
    boolean degraded = false;
    String fatal = null;
    String status = "ok";

    try {
      ElectionConfig config;
      try {
        config = adapter.getElectionConfig();
        stats.record(0, "ok");
      } catch (TseClient.NotFoundException e) {
        stats.record(0, "not-found");
        store.finishCycle(cycle, "waiting", e.getMessage(), stats);
        return "waiting";
      }

      String progressCode = config.progressElectionCode();
      Instant now = Instant.now();

      try {
        Fetched<CountryProgress> progress = adapter.getCountryProgress(progressCode);
        if (progress.changed()) {
          stats.record(0, "ok");
          List<AreaProgressView> entries = new ArrayList<>();
          entries.add(
              new AreaProgressView("br", "country", null, progress.data().progress()));
          // MVP: UFs do arquivo nacional (sem município)
          entries.addAll(progress.data().states());
          store.applyProgress(round.getId(), entries, progress.provenance(), now);
          String st = progress.data().progress().status();
          if (!"not-started".equals(st)) {
            store.setRoundStatus(round, "finished".equals(st) ? "final" : "live");
          }
        } else {
          stats.record(0, "not-modified");
        }
      } catch (TseClient.NotFoundException e) {
        stats.record(0, "not-found");
        degraded = true;
      } catch (TseClient.UnavailableException | TsePayloadException e) {
        stats.record(0, "error");
        degraded = true;
        log.warn("Erro ao buscar progresso BR (mantendo último estado): {}", e.getMessage());
      }

      // MVP: só presidente nacional; sem município
      Optional<TseOffice> presidente =
          config.offices().stream().filter(o -> "1".equals(o.code()) || "presidente".equals(o.slug())).findFirst();
      Optional<Office> dbOffice =
          officeRepository.findByRoundIdAndSlug(round.getId(), "presidente");

      if (presidente.isPresent() && dbOffice.isPresent()) {
        try {
          Fetched<AreaResultView> result = adapter.getCountryResult(presidente.get());
          if (result.changed()) {
            stats.record(0, "ok");
            store.applyResult(round.getId(), dbOffice.get(), result.data(), result.provenance(), Instant.now());
          } else {
            stats.record(0, "not-modified");
          }
        } catch (TseClient.NotFoundException e) {
          stats.record(0, "not-found");
          degraded = true;
        } catch (TseClient.UnavailableException | TsePayloadException e) {
          stats.record(0, "error");
          degraded = true;
          log.warn("Erro ao buscar resultado BR (mantendo último estado): {}", e.getMessage());
        }
      } else {
        log.warn("Cargo presidente não encontrado no config TSE ou no banco");
        degraded = true;
      }

      if (properties.isCollectCityResults()) {
        log.debug("COLLECT_CITY_RESULTS=true ainda não implementado no MVP — ignorado");
      }

      status = degraded ? "degraded" : "ok";
      store.finishCycle(cycle, status, null, stats);
      return status;
    } catch (Exception e) {
      fatal = e.getMessage();
      log.error("Ciclo collector falhou: {}", fatal, e);
      stats.record(0, "error");
      store.finishCycle(cycle, "failed", fatal, stats);
      return "failed";
    }
  }
}
