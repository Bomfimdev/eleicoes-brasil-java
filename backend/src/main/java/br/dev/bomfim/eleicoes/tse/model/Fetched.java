package br.dev.bomfim.eleicoes.tse.model;

public record Fetched<T>(boolean changed, T data, Provenance provenance) {

  public static <T> Fetched<T> unchanged() {
    return new Fetched<>(false, null, null);
  }

  public static <T> Fetched<T> of(T data, Provenance provenance) {
    return new Fetched<>(true, data, provenance);
  }
}
