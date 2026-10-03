package br.dev.bomfim.eleicoes.api;

import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

  @Value("${eleicoes.app-mode:DEVELOPMENT}")
  private String appMode = "DEVELOPMENT";

  @GetMapping("/health")
  public Map<String, Object> health() {
    return Map.of(
        "status", "ok",
        "service", "eleicoes-api",
        "mode", appMode == null ? "DEVELOPMENT" : appMode,
        "timestamp", Instant.now().toString());
  }
}
