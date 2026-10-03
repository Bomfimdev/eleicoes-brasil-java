package br.dev.bomfim.eleicoes.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "area_results")
@IdClass(AreaResultId.class)
public class AreaResult {

  @Id
  @Column(name = "round_id", nullable = false)
  private UUID roundId;

  @Id
  @Column(name = "office_id", nullable = false)
  private UUID officeId;

  @Id
  @Column(name = "area_key", nullable = false)
  private String areaKey;

  @Column(name = "area_type", nullable = false)
  private String areaType;

  @Column(name = "state_code")
  private String stateCode;

  @Column(name = "counted_pct")
  private Double countedPct;

  @Column(name = "totalized_at")
  private Instant totalizedAt;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private String result;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "previous_candidates", columnDefinition = "jsonb")
  private String previousCandidates;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private String provenance;

  @Column(nullable = false)
  private String checksum;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
