package br.dev.bomfim.eleicoes;

import static org.assertj.core.api.Assertions.assertThat;

import br.dev.bomfim.eleicoes.domain.ElectionRepository;
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
class EleicoesApiApplicationTests {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("eleicoes")
          .withUsername("eleicoes")
          .withPassword("eleicoes");

  @DynamicPropertySource
  static void datasourceProps(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.liquibase.user", postgres::getUsername);
    registry.add("spring.liquibase.password", postgres::getPassword);
  }

  @Autowired
  private ElectionRepository electionRepository;

  @Test
  void contextLoadsAndDemoSeedIsPresent() {
    assertThat(electionRepository.existsBySlug("demo")).isTrue();
    assertThat(electionRepository.findBySlug("demo"))
        .isPresent()
        .get()
        .satisfies(
            election -> {
              assertThat(election.isDemo()).isTrue();
              assertThat(election.getName()).containsIgnoringCase("fict");
            });
  }
}
