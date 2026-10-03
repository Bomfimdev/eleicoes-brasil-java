package br.dev.bomfim.eleicoes.tse;

/** Cliente de leitura dos arquivos JSON do TSE (HTTP ou classpath). */
public interface TseClient {

  FetchResult get(String url);

  default void resetValidators() {}

  record FetchResult(
      boolean notModified, String body, String etag, String checksum, long durationMs) {

    public static FetchResult notModified(long durationMs) {
      return new FetchResult(true, null, null, null, durationMs);
    }

    public static FetchResult ok(String body, String etag, String checksum, long durationMs) {
      return new FetchResult(false, body, etag, checksum, durationMs);
    }
  }

  class NotFoundException extends RuntimeException {
    public NotFoundException(String url) {
      super("TSE 404: " + url);
    }
  }

  class UnavailableException extends RuntimeException {
    private final Integer status;

    public UnavailableException(String message, String url, Integer status) {
      super(message + " (" + url + ")");
      this.status = status;
    }

    public Integer getStatus() {
      return status;
    }
  }
}
