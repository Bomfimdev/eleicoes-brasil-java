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
@Table(name = "candidates")
public class Candidate {

  @Id
  private UUID id;

  @Column(name = "round_id", nullable = false)
  private UUID roundId;

  @Column(name = "office_id", nullable = false)
  private UUID officeId;

  @Column(name = "state_code")
  private String stateCode;

  @Column(nullable = false)
  private String provider;

  @Column(name = "provider_id", nullable = false)
  private String providerId;

  @Column(nullable = false)
  private String number;

  @Column(nullable = false)
  private String name;

  @Column(name = "ballot_name", nullable = false)
  private String ballotName;

  @Column(name = "search_name", nullable = false)
  private String searchName;

  @Column(name = "party_number", nullable = false)
  private String partyNumber;

  @Column(name = "party_abbreviation", nullable = false)
  private String partyAbbreviation;

  private String coalition;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "running_mates", nullable = false, columnDefinition = "jsonb")
  private String runningMates;
}
