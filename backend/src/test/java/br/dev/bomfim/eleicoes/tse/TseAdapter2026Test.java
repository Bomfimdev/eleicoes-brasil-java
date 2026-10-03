package br.dev.bomfim.eleicoes.tse;

import static org.assertj.core.api.Assertions.assertThat;

import br.dev.bomfim.eleicoes.tse.model.AreaResultView;
import br.dev.bomfim.eleicoes.tse.model.CountryProgress;
import br.dev.bomfim.eleicoes.tse.model.ElectionConfig;
import br.dev.bomfim.eleicoes.tse.model.Fetched;
import br.dev.bomfim.eleicoes.tse.model.TseOffice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class TseAdapter2026Test {

  private TseAdapter2026 adapter;

  @BeforeEach
  void setUp() {
    JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
    adapter =
        new TseAdapter2026(
            new ClasspathTseClient("fixtures/tse"),
            mapper,
            "fixture",
            "local",
            "17801",
            1,
            "2026-04-26");
  }

  @Test
  void loadsConfigFromEleC() {
    ElectionConfig config = adapter.getElectionConfig();
    assertThat(config.providerRoundId()).isEqualTo("17801");
    assertThat(config.progressElectionCode()).isEqualTo("21270");
    assertThat(config.offices())
        .extracting(TseOffice::slug)
        .contains("presidente", "governador");
  }

  @Test
  void convertsCountryProgress() {
    adapter.getElectionConfig();
    Fetched<CountryProgress> fetched = adapter.getCountryProgress("21270");
    assertThat(fetched.changed()).isTrue();
    assertThat(fetched.data().progress().status()).isIn("not-started", "in-progress", "finished");
    assertThat(fetched.data().progress().sectionsCountedPct()).isNotNull();
    assertThat(fetched.data().progress().totalizedAt()).isNotNull();
    assertThat(fetched.provenance().checksum()).isNotBlank();
  }

  @Test
  void convertsCountryResultWithPvapn() {
    ElectionConfig config = adapter.getElectionConfig();
    TseOffice presidente =
        config.offices().stream().filter(o -> "presidente".equals(o.slug())).findFirst().orElseThrow();
    Fetched<AreaResultView> fetched = adapter.getCountryResult(presidente);
    assertThat(fetched.changed()).isTrue();
    assertThat(fetched.data().candidates()).isNotEmpty();
    assertThat(fetched.data().candidates().get(0).percent()).isNotNull();
    assertThat(fetched.data().candidates().get(0).votes()).isNotNull();
    assertThat(fetched.data().areaKey()).isEqualTo("br");
  }
}
