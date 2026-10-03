package br.dev.bomfim.eleicoes.collector;

import static org.assertj.core.api.Assertions.assertThat;

import br.dev.bomfim.eleicoes.domain.AreaProgressRepository;
import br.dev.bomfim.eleicoes.domain.AreaResultRepository;
import br.dev.bomfim.eleicoes.domain.CollectorCycleRepository;
import br.dev.bomfim.eleicoes.domain.ElectionRoundRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class CollectorCycleIT {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("eleicoes")
          .withUsername("eleicoes")
          .withPassword("eleicoes");

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.liquibase.user", postgres::getUsername);
    registry.add("spring.liquibase.password", postgres::getPassword);
    registry.add("eleicoes.app-mode", () -> "DEVELOPMENT");
    registry.add("eleicoes.collector.enabled", () -> "true");
    registry.add("eleicoes.collector.ignore-windows", () -> "true");
    registry.add("eleicoes.collector.round-slug", () -> "demo-1");
  }

  @Autowired private CollectorService collectorService;
  @Autowired private AreaProgressRepository progressRepository;
  @Autowired private AreaResultRepository resultRepository;
  @Autowired private CollectorCycleRepository cycleRepository;
  @Autowired private ElectionRoundRepository roundRepository;

  @Test
  void developmentCyclePersistsProgressAndResults() {
    String status = collectorService.runCycle();
    assertThat(status).isIn("ok", "degraded");

    UUID roundId = roundRepository.findBySlug("demo-1").orElseThrow().getId();
    assertThat(progressRepository.findByRoundIdAndAreaKey(roundId, "br")).isPresent();
    assertThat(resultRepository.findByRoundIdAndAreaKey(roundId, "br")).isNotEmpty();
    assertThat(cycleRepository.findAll()).isNotEmpty();
    assertThat(cycleRepository.findAll().get(0).getStatus()).isIn("ok", "degraded");
  }
}
