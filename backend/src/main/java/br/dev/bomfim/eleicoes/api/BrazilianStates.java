package br.dev.bomfim.eleicoes.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** UFs brasileiras + pseudo-UF ZZ (Exterior, só presidente). */
public final class BrazilianStates {

  public record State(String code, String name, String region) {}

  public static final State EXTERIOR = new State("ZZ", "Exterior", "EX");

  /** 26 estados + DF (sem Exterior). */
  public static final List<State> DOMESTIC =
      List.of(
          new State("AC", "Acre", "N"),
          new State("AL", "Alagoas", "NE"),
          new State("AP", "Amapá", "N"),
          new State("AM", "Amazonas", "N"),
          new State("BA", "Bahia", "NE"),
          new State("CE", "Ceará", "NE"),
          new State("DF", "Distrito Federal", "CO"),
          new State("ES", "Espírito Santo", "SE"),
          new State("GO", "Goiás", "CO"),
          new State("MA", "Maranhão", "NE"),
          new State("MT", "Mato Grosso", "CO"),
          new State("MS", "Mato Grosso do Sul", "CO"),
          new State("MG", "Minas Gerais", "SE"),
          new State("PA", "Pará", "N"),
          new State("PB", "Paraíba", "NE"),
          new State("PR", "Paraná", "S"),
          new State("PE", "Pernambuco", "NE"),
          new State("PI", "Piauí", "NE"),
          new State("RJ", "Rio de Janeiro", "SE"),
          new State("RN", "Rio Grande do Norte", "NE"),
          new State("RS", "Rio Grande do Sul", "S"),
          new State("RO", "Rondônia", "N"),
          new State("RR", "Roraima", "N"),
          new State("SC", "Santa Catarina", "S"),
          new State("SP", "São Paulo", "SE"),
          new State("SE", "Sergipe", "NE"),
          new State("TO", "Tocantins", "N"));

  /** Alias histórico: só UFs domésticas (mapa / calor). */
  public static final List<State> ALL = DOMESTIC;

  /** Domésticas + Exterior (ZZ). */
  public static final List<State> WITH_EXTERIOR;

  static {
    List<State> all = new ArrayList<>(DOMESTIC);
    all.add(EXTERIOR);
    WITH_EXTERIOR = List.copyOf(all);
  }

  private static final Map<String, State> BY_CODE =
      WITH_EXTERIOR.stream().collect(java.util.stream.Collectors.toMap(State::code, s -> s));

  private BrazilianStates() {}

  public static State require(String uf) {
    State s = BY_CODE.get(uf.toUpperCase());
    if (s == null) {
      throw new ApiNotFoundException("UF desconhecida: " + uf);
    }
    return s;
  }

  public static boolean isValid(String uf) {
    return uf != null && BY_CODE.containsKey(uf.toUpperCase());
  }

  public static boolean isExterior(String uf) {
    return uf != null && "ZZ".equalsIgnoreCase(uf);
  }

  public static boolean isDomestic(String uf) {
    return isValid(uf) && !isExterior(uf);
  }
}
