package br.dev.bomfim.eleicoes.collector;

import br.dev.bomfim.eleicoes.api.BrazilianStates;
import br.dev.bomfim.eleicoes.api.RealtimeVersionHub;
import br.dev.bomfim.eleicoes.config.EleicoesProperties;
import br.dev.bomfim.eleicoes.domain.City;
import br.dev.bomfim.eleicoes.domain.CityRepository;
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
import br.dev.bomfim.eleicoes.tse.model.StateProgress;
import br.dev.bomfim.eleicoes.tse.model.TseOffice;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
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
  private final CityRepository cityRepository;
  private final TseAdapter2026 adapter;
  private final RealtimeVersionHub versionHub;
  private final ReplayService replayService;

  public CollectorService(
      EleicoesProperties properties,
      ElectionWindowGate gate,
      CollectorStore store,
      OfficeRepository officeRepository,
      CityRepository cityRepository,
      TseAdapter2026 adapter,
      RealtimeVersionHub versionHub,
      ReplayService replayService) {
    this.properties = properties;
    this.gate = gate;
    this.store = store;
    this.officeRepository = officeRepository;
    this.cityRepository = cityRepository;
    this.adapter = adapter;
    this.versionHub = versionHub;
    this.replayService = replayService;
  }

  /** @return status do ciclo: skipped, ok, degraded, failed, waiting, finished */
  public String runCycle() {
    if (!properties.getCollector().isEnabled()) {
      return "skipped";
    }
    String mode = properties.getAppMode() == null ? "DEVELOPMENT" : properties.getAppMode().toUpperCase();
    if ("REPLAY".equals(mode)) {
      return replayService.runTick();
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
    CollectorCycle cycle = store.startCycle(round.getId(), mode);
    CollectorStore.CycleStats stats = new CollectorStore.CycleStats();
    boolean degraded = false;
    String fatal = null;
    String status = "ok";
    boolean changed = false;

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
          entries.add(new AreaProgressView("br", "country", null, progress.data().progress()));
          entries.addAll(progress.data().states());
          int n = store.applyProgress(round.getId(), entries, progress.provenance(), now);
          if (n > 0) {
            changed = true;
          }
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

      Optional<TseOffice> presidente =
          config.offices().stream()
              .filter(o -> "1".equals(o.code()) || "presidente".equals(o.slug()))
              .findFirst();
      Optional<Office> dbOffice = officeRepository.findByRoundIdAndSlug(round.getId(), "presidente");

      if (presidente.isPresent() && dbOffice.isPresent()) {
        TseOffice tseOffice = presidente.get();
        Office office = dbOffice.get();
        try {
          Fetched<AreaResultView> result = adapter.getCountryResult(tseOffice);
          if (result.changed()) {
            stats.record(0, "ok");
            if (store.applyResult(round.getId(), office, result.data(), result.provenance(), Instant.now())) {
              changed = true;
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
          log.warn("Erro ao buscar resultado BR (mantendo último estado): {}", e.getMessage());
        }

        for (BrazilianStates.State state : BrazilianStates.ALL) {
          try {
            Fetched<AreaResultView> stateResult = adapter.getStateResult(tseOffice, state.code());
            if (stateResult.changed()) {
              stats.record(0, "ok");
              if (store.applyResult(
                  round.getId(), office, stateResult.data(), stateResult.provenance(), Instant.now())) {
                changed = true;
              }
            } else {
              stats.record(0, "not-modified");
            }
          } catch (TseClient.NotFoundException e) {
            stats.record(0, "not-found");
          } catch (TseClient.UnavailableException | TsePayloadException e) {
            stats.record(0, "error");
            degraded = true;
            log.debug("Erro resultado UF {}: {}", state.code(), e.getMessage());
          }
        }

        if (properties.isCollectCityResults()) {
          List<City> capitals = cityRepository.findByProviderAndCapitalTrueOrderByStateCodeAsc("TSE");
          Set<String> capitalKeys = new HashSet<>();
          for (City c : capitals) {
            capitalKeys.add(c.getStateCode().toLowerCase(Locale.ROOT) + "-" + c.getProviderId());
          }
          Set<String> progressDone = new HashSet<>();
          for (City capital : capitals) {
            String uf = capital.getStateCode();
            if (progressDone.add(uf)) {
              try {
                Fetched<StateProgress> sp = adapter.getStateProgress(progressCode, uf);
                if (sp.changed()) {
                  stats.record(0, "ok");
                  List<AreaProgressView> cityProgress =
                      sp.data().cities().stream()
                          .filter(p -> capitalKeys.contains(p.areaKey()))
                          .toList();
                  if (!cityProgress.isEmpty()) {
                    int n =
                        store.applyProgress(
                            round.getId(), cityProgress, sp.provenance(), Instant.now());
                    if (n > 0) {
                      changed = true;
                    }
                  }
                } else {
                  stats.record(0, "not-modified");
                }
              } catch (TseClient.NotFoundException e) {
                stats.record(0, "not-found");
              } catch (TseClient.UnavailableException | TsePayloadException e) {
                stats.record(0, "error");
                log.debug("Erro progresso cidades {}: {}", uf, e.getMessage());
              }
            }

            try {
              Fetched<AreaResultView> cityResult =
                  adapter.getCityResult(tseOffice, uf, capital.getProviderId());
              if (cityResult.changed()) {
                stats.record(0, "ok");
                if (store.applyResult(
                    round.getId(), office, cityResult.data(), cityResult.provenance(), Instant.now())) {
                  changed = true;
                }
              } else {
                stats.record(0, "not-modified");
              }
            } catch (TseClient.NotFoundException e) {
              stats.record(0, "not-found");
            } catch (TseClient.UnavailableException | TsePayloadException e) {
              stats.record(0, "error");
              log.debug("Erro resultado capital {}-{}: {}", uf, capital.getProviderId(), e.getMessage());
            }
          }
        }
      } else {
        log.warn("Cargo presidente não encontrado no config TSE ou no banco");
        degraded = true;
      }

      status = degraded ? "degraded" : "ok";
      store.finishCycle(cycle, status, null, stats);
      if (changed) {
        versionHub.publish(round.getSlug());
      }
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
