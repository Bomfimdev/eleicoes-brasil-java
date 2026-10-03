package br.dev.bomfim.eleicoes.tse;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Horários do TSE são em Brasília sem offset. Desde 2019 Brasilia = UTC−03:00 fixo.
 */
public final class TseTime {

  private static final Pattern DATE = Pattern.compile("^(\\d{2})/(\\d{2})/(\\d{4})$");
  private static final Pattern TIME = Pattern.compile("^(\\d{2}):(\\d{2}):(\\d{2})$");
  private static final ZoneOffset BRASILIA = ZoneOffset.of("-03:00");

  private TseTime() {}

  /** "04/10/2026" + "20:43:12" → Instant UTC, ou null se inválido. */
  public static Instant brasiliaToUtc(String date, String time) {
    if (date == null || date.isBlank()) {
      return null;
    }
    Matcher d = DATE.matcher(date.trim());
    if (!d.matches()) {
      return null;
    }
    String tRaw = (time == null || time.isBlank()) ? "00:00:00" : time.trim();
    Matcher t = TIME.matcher(tRaw);
    if (!t.matches()) {
      return null;
    }
    LocalDateTime ldt =
        LocalDateTime.of(
            Integer.parseInt(d.group(3)),
            Integer.parseInt(d.group(2)),
            Integer.parseInt(d.group(1)),
            Integer.parseInt(t.group(1)),
            Integer.parseInt(t.group(2)),
            Integer.parseInt(t.group(3)));
    return ldt.toInstant(BRASILIA);
  }

  /** "26/04/2026" → "2026-04-26", ou null. */
  public static String isoDate(String ddmmyyyy) {
    if (ddmmyyyy == null) {
      return null;
    }
    Matcher m = DATE.matcher(ddmmyyyy.trim());
    if (!m.matches()) {
      return null;
    }
    return m.group(3) + "-" + m.group(2) + "-" + m.group(1);
  }
}
