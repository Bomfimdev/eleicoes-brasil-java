package br.dev.bomfim.eleicoes.collector;

import br.dev.bomfim.eleicoes.config.EleicoesProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CollectorScheduler {

  private static final Logger log = LoggerFactory.getLogger(CollectorScheduler.class);

  private final CollectorService collectorService;
  private final EleicoesProperties properties;

  public CollectorScheduler(CollectorService collectorService, EleicoesProperties properties) {
    this.collectorService = collectorService;
    this.properties = properties;
  }

  @Scheduled(
      fixedDelayString = "${eleicoes.tse.poll-interval-ms:15000}",
      initialDelayString = "${eleicoes.tse.poll-interval-ms:15000}")
  public void tick() {
    if (!properties.getCollector().isEnabled()) {
      return;
    }
    String status = collectorService.runCycle();
    if (!"skipped".equals(status)) {
      log.info("Collector cycle status={}", status);
    }
  }
}
