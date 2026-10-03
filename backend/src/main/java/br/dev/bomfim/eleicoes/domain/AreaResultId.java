package br.dev.bomfim.eleicoes.domain;

import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class AreaResultId implements Serializable {

  private UUID roundId;
  private UUID officeId;
  private String areaKey;
}
