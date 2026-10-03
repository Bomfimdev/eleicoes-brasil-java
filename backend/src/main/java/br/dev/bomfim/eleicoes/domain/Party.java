package br.dev.bomfim.eleicoes.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "parties")
@IdClass(PartyId.class)
public class Party {

  @Id
  @Column(name = "round_id", nullable = false)
  private UUID roundId;

  @Id
  @Column(nullable = false)
  private String number;

  @Column(nullable = false)
  private String abbreviation;

  @Column(nullable = false)
  private String name;
}
