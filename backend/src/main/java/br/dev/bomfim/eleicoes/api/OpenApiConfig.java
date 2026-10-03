package br.dev.bomfim.eleicoes.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  OpenAPI eleicoesOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Eleições Brasil API")
                .version("0.3.0")
                .description(
                    "API read-only de apuração. Projeto independente — não é serviço oficial do TSE."));
  }
}
