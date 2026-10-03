package br.dev.bomfim.eleicoes.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class HealthControllerTest {

  @Test
  void healthReturnsOk() {
    HealthController controller = new HealthController();
    Map<String, Object> body = controller.health();

    assertThat(body.get("status")).isEqualTo("ok");
    assertThat(body.get("service")).isEqualTo("eleicoes-api");
  }
}
