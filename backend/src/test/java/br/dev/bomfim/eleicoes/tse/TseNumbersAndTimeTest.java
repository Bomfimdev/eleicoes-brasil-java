package br.dev.bomfim.eleicoes.tse;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class TseNumbersAndTimeTest {

  @Test
  void toDecAcceptsBrazilianComma() {
    assertThat(TseNumbers.toDec("7,52")).isEqualTo(7.52);
    assertThat(TseNumbers.toDec("48.321234567")).isEqualTo(48.321234567);
    assertThat(TseNumbers.toDec("")).isNull();
    assertThat(TseNumbers.toDec(null)).isNull();
  }

  @Test
  void toLongRejectsGarbage() {
    assertThat(TseNumbers.toLong("52381292")).isEqualTo(52381292L);
    assertThat(TseNumbers.toLong("12,3")).isNull();
    assertThat(TseNumbers.toLong("")).isNull();
  }

  @Test
  void brasiliaToUtc() {
    Instant utc = TseTime.brasiliaToUtc("04/10/2026", "20:43:12");
    assertThat(utc).isEqualTo(Instant.parse("2026-10-04T23:43:12Z"));
    assertThat(TseTime.brasiliaToUtc("", "20:00:00")).isNull();
    assertThat(TseTime.brasiliaToUtc("2026-10-04", "20:00:00")).isNull();
  }
}
