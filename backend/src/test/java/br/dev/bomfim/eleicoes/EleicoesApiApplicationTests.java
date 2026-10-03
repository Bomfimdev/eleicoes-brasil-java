package br.dev.bomfim.eleicoes;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requer PostgreSQL local — ativar com Testcontainers na Fase 1")
class EleicoesApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
