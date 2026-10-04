package br.dev.bomfim.eleicoes.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class ElectionApiIT {

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
    registry.add("eleicoes.collector.enabled", () -> "false");
  }

  @Autowired private MockMvc mockMvc;

  @Test
  void electionsAndOverviewFromSeed() throws Exception {
    mockMvc
        .perform(get("/api/elections"))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "public, max-age=15"))
        .andExpect(jsonPath("$[0].slug").value("2026"))
        .andExpect(jsonPath("$[0].demo").value(false))
        .andExpect(jsonPath("$[0].rounds[0].slug").value("2026-1"));

    mockMvc
        .perform(get("/api/elections/2026-1/overview"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.round.slug").value("2026-1"))
        .andExpect(jsonPath("$.round.electionName").value("Eleições Gerais 2026"))
        .andExpect(jsonPath("$.progress.status").value("waiting"))
        .andExpect(jsonPath("$.headline.officeSlug").value("presidente"))
        .andExpect(jsonPath("$.states").isArray());

    mockMvc
        .perform(get("/api/elections/2026-1/results").param("office", "presidente").param("area", "br"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.areaKey").value("br"));

    mockMvc
        .perform(get("/api/elections/2026-1/operations"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ingestion.state").exists())
        .andExpect(jsonPath("$.events").isArray());

    mockMvc
        .perform(get("/api/elections/2026-1/timeline"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.points").isArray());

    mockMvc
        .perform(get("/api/elections/2026-1/compare").param("states", "SP,MG,RJ"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.states").isArray())
        .andExpect(jsonPath("$.states[0].uf").value("SP"));

    mockMvc
        .perform(get("/api/elections/2026-1/series").param("office", "presidente").param("area", "br"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.areaKey").value("br"))
        .andExpect(jsonPath("$.points").isArray());

    mockMvc
        .perform(get("/api/elections/2026-1/states/SP/cities"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].capital").value(true))
        .andExpect(jsonPath("$.items[0].code").value("71072"));

    mockMvc
        .perform(get("/api/elections/2026-1/states/ZZ"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.uf").value("ZZ"))
        .andExpect(jsonPath("$.name").value("Exterior"));
  }

  @Test
  void metaAndSwaggerAvailable() throws Exception {
    mockMvc
        .perform(get("/api/meta"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.timezone").value("America/Sao_Paulo"));

    MvcResult docs = mockMvc.perform(get("/api-docs")).andExpect(status().isOk()).andReturn();
    assertThat(docs.getResponse().getContentAsString()).contains("Eleições Brasil API");
  }
}
