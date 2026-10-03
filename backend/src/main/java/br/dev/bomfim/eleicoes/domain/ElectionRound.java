package br.dev.bomfim.eleicoes.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "election_rounds")
public class ElectionRound {

  @Id
  private UUID id;

  @Column(name = "election_id", nullable = false)
  private UUID electionId;

  @Column(nullable = false, unique = true)
  private String slug;

  @Column(nullable = false)
  private short round;

  @Column(nullable = false)
  private LocalDate date;

  @Column(nullable = false)
  private String status;

  @Column(nullable = false)
  private String provider;

  @Column(name = "provider_id")
  private String providerId;

  private String adapter;

  @Column(name = "adapter_version")
  private String adapterVersion;

  private String environment;

  private String mode;

  @Column(name = "progress_election_code")
  private String progressElectionCode;

  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(name = "provider_election_codes", columnDefinition = "text[]")
  private String[] providerElectionCodes;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
