package br.dev.bomfim.eleicoes.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "result_snapshots")
public class ResultSnapshot {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "round_id", nullable = false)
  private UUID roundId;

  @Column(name = "office_id", nullable = false)
  private UUID officeId;

  @Column(name = "area_key", nullable = false)
  private String areaKey;

  @Column(name = "area_type", nullable = false)
  private String areaType;

  @Column(name = "state_code")
  private String stateCode;

  @Column(name = "captured_at", nullable = false)
  private Instant capturedAt;

  @Column(name = "totalized_at")
  private Instant totalizedAt;

  @Column(name = "counted_pct")
  private Double countedPct;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private String votes;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private String candidates;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private String provenance;
}
