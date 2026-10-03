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
@Table(name = "progress_snapshots")
public class ProgressSnapshot {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "round_id", nullable = false)
  private UUID roundId;

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

  @Column(name = "sections_counted")
  private Integer sectionsCounted;

  private Long turnout;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private String progress;

  @Column(name = "source_file")
  private String sourceFile;

  @Column(name = "source_id")
  private String sourceId;
}
