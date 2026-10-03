package br.dev.bomfim.eleicoes.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

class HealthControllerTest {

  @Test
  void healthReturnsOkWithoutTouchingDatabase() {
    HealthController controller = new HealthController(mock(JdbcTemplate.class), "DEVELOPMENT");
    Map<String, Object> body = controller.health();

    assertThat(body.get("status")).isEqualTo("ok");
    assertThat(body.get("service")).isEqualTo("eleicoes-api");
  }

  @Test
  void readyReturnsOkWhenDatabaseAnswers() {
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    when(jdbc.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);

    HealthController controller = new HealthController(jdbc, "DEVELOPMENT");
    ResponseEntity<Map<String, Object>> response = controller.ready();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("status")).isEqualTo("ready");
    assertThat(response.getBody().get("database")).isEqualTo("up");
  }

  @Test
  void readyReturns503WhenDatabaseFails() {
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    when(jdbc.queryForObject(anyString(), eq(Integer.class)))
        .thenThrow(new RuntimeException("connection refused"));

    HealthController controller = new HealthController(jdbc, "DEVELOPMENT");
    ResponseEntity<Map<String, Object>> response = controller.ready();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("status")).isEqualTo("not_ready");
  }
}
