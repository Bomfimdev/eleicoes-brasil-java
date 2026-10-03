package br.dev.bomfim.eleicoes.tse;

public class TsePayloadException extends RuntimeException {

  public TsePayloadException(String message, String url) {
    super(message + " @ " + url);
  }
}
