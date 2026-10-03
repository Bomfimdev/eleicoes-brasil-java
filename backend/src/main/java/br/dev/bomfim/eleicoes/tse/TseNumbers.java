package br.dev.bomfim.eleicoes.tse;

/** Conversões de números no formato TSE (texto com vírgula decimal). */
public final class TseNumbers {

  private TseNumbers() {}

  /** "1234" | 1234 → 1234. Qualquer outra coisa → null (nunca NaN). */
  public static Long toLong(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number n) {
      return Double.isFinite(n.doubleValue()) ? n.longValue() : null;
    }
    String s = String.valueOf(value).trim();
    if (s.isEmpty()) {
      return null;
    }
    if (!s.matches("^-?\\d+$")) {
      return null;
    }
    return Long.parseLong(s);
  }

  public static Integer toInt(Object value) {
    Long n = toLong(value);
    return n == null ? null : n.intValue();
  }

  /** "48,43" | "48.431" | 48.43 → 48.43. Qualquer outra coisa → null. */
  public static Double toDec(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number n) {
      return Double.isFinite(n.doubleValue()) ? n.doubleValue() : null;
    }
    String s = String.valueOf(value).trim();
    if (s.isEmpty()) {
      return null;
    }
    if (s.contains(",")) {
      s = s.replace(".", "").replace(',', '.');
    }
    if (!s.matches("^-?\\d+(\\.\\d+)?$")) {
      return null;
    }
    return Double.parseDouble(s);
  }

  public static String asCode(Object value) {
    if (value == null) {
      return null;
    }
    return String.valueOf(value);
  }
}
