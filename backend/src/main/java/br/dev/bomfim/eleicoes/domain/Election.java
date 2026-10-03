package br.dev.bomfim.eleicoes.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "elections")
public class Election {

  @Id
  private UUID id;

  @Column(nullable = false, unique = true)
  private String slug;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private short year;

  @Column(nullable = false)
  private String kind;

  @Column(nullable = false)
  private boolean demo;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
