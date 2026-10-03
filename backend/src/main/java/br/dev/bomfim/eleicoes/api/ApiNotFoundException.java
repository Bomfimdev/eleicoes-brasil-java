package br.dev.bomfim.eleicoes.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ApiNotFoundException extends RuntimeException {

  public ApiNotFoundException(String message) {
    super(message);
  }
}
