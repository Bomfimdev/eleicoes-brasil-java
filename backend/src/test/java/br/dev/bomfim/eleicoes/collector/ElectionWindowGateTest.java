package br.dev.bomfim.eleicoes.collector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.dev.bomfim.eleicoes.config.EleicoesProperties;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ElectionWindowGateTest {

  @Test
  void parseMultipleWindows() {
    List<ElectionWindowGate.Window> windows =
        ElectionWindowGate.parse(
            "2026-10-04T17:00-03:00/2026-10-05T22:00-03:00,2026-10-25T17:00-03:00/2026-10-26T22:00-03:00");
    assertThat(windows).hasSize(2);
    assertThat(windows.get(0).start()).isEqualTo(OffsetDateTime.parse("2026-10-04T17:00-03:00"));
    assertThat(windows.get(1).end()).isEqualTo(OffsetDateTime.parse("2026-10-26T22:00-03:00"));
  }

  @Test
  void parseRejectsInvalid() {
    assertThatThrownBy(() -> ElectionWindowGate.parse("sem-barra"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void ignoreWindowsAlwaysOpen() {
    EleicoesProperties props = new EleicoesProperties();
    props.setElectionWindows("");
    props.getCollector().setIgnoreWindows(true);
    ElectionWindowGate gate = new ElectionWindowGate(props);
    assertThat(gate.isOpen()).isTrue();
  }

  @Test
  void emptyWindowsClosedWhenNotIgnoring() {
    EleicoesProperties props = new EleicoesProperties();
    props.setElectionWindows("");
    props.getCollector().setIgnoreWindows(false);
    ElectionWindowGate gate = new ElectionWindowGate(props);
    assertThat(gate.isOpen()).isFalse();
  }
}
