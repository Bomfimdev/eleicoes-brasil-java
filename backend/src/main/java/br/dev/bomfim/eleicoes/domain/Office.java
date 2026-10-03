package br.dev.bomfim.eleicoes.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "offices")
public class Office {

  @Id
  private UUID id;

  @Column(name = "round_id", nullable = false)
  private UUID roundId;

  @Column(nullable = false)
  private String provider;

  @Column(name = "provider_id", nullable = false)
  private String providerId;

  @Column(name = "provider_election_code", nullable = false)
  private String providerElectionCode;

  @Column(nullable = false)
  private String slug;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String kind;

  @Column(nullable = false)
  private String scope;

  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(columnDefinition = "text[]")
  private String[] states;
}
