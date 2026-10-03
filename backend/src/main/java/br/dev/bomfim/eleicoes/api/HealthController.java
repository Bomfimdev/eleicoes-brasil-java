package br.dev.bomfim.eleicoes.api;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

  private final JdbcTemplate jdbcTemplate;
  private final String appMode;

  public HealthController(
      JdbcTemplate jdbcTemplate, @Value("${eleicoes.app-mode:DEVELOPMENT}") String appMode) {
    this.jdbcTemplate = jdbcTemplate;
    this.appMode = appMode == null ? "DEVELOPMENT" : appMode;
  }

  /** Leve, sem banco — usado pelo cron-job.org para acordar a API. */
  @GetMapping("/health")
  public Map<String, Object> health() {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("status", "ok");
    body.put("service", "eleicoes-api");
    body.put("mode", appMode);
    body.put("timestamp", Instant.now().toString());
    return body;
  }

  /** Verifica conexão com o banco. */
  @GetMapping("/ready")
  public ResponseEntity<Map<String, Object>> ready() {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("service", "eleicoes-api");
    body.put("mode", appMode);
    body.put("timestamp", Instant.now().toString());
    try {
      Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
      body.put("status", "ready");
      body.put("database", one != null && one == 1 ? "up" : "unknown");
      return ResponseEntity.ok(body);
    } catch (Exception ex) {
      body.put("status", "not_ready");
      body.put("database", "down");
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
  }
}
