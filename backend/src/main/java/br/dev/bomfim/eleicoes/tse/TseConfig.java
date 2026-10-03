package br.dev.bomfim.eleicoes.tse;

import br.dev.bomfim.eleicoes.config.EleicoesProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class TseConfig {

  @Bean
  public TseClient tseClient(EleicoesProperties properties) {
    String mode = properties.getAppMode() == null ? "DEVELOPMENT" : properties.getAppMode().toUpperCase();
    if ("SIMULATION".equals(mode) || "PRODUCTION".equals(mode)) {
      return new TseHttpClient(properties);
    }
    return new ClasspathTseClient(properties);
  }

  @Bean
  public TseAdapter2026 tseAdapter2026(TseClient client, JsonMapper mapper, EleicoesProperties properties) {
    Source source = resolveSource(properties);
    return new TseAdapter2026(
        client,
        mapper,
        source.baseUrl(),
        source.environment(),
        source.providerRoundId(),
        1,
        "2026-10-04");
  }

  static Source resolveSource(EleicoesProperties properties) {
    String mode = properties.getAppMode() == null ? "DEVELOPMENT" : properties.getAppMode().toUpperCase();
    String base = properties.getTse().getBaseUrl();
    String env = properties.getTse().getEnvironment();
    String roundId = properties.getTse().getProviderRoundId();
    return switch (mode) {
      case "SIMULATION" ->
          new Source(
              blank(base, "https://resultados-sim.tse.jus.br/simulado"),
              blank(env, "simulado2026"),
              blank(roundId, "17801"));
      case "PRODUCTION" ->
          new Source(
              blank(base, "https://resultados.tse.jus.br"),
              blank(env, "oficial"),
              blank(roundId, "3220"));
      default ->
          new Source(
              blank(base, "fixture"),
              blank(env, "local"),
              blank(roundId, "17801"));
    };
  }

  private static String blank(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  record Source(String baseUrl, String environment, String providerRoundId) {}
}
