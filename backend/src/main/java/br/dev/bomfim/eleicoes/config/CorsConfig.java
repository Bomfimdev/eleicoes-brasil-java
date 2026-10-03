package br.dev.bomfim.eleicoes.config;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

  @Bean
  CorsFilter corsFilter(@Value("${eleicoes.cors-origins}") String corsOrigins) {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowCredentials(true);
    Arrays.stream(corsOrigins.split(","))
        .map(String::trim)
        .filter(origin -> !origin.isEmpty())
        .forEach(config::addAllowedOrigin);
    config.addAllowedHeader("*");
    config.addAllowedMethod("*");

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return new CorsFilter(source);
  }
}
