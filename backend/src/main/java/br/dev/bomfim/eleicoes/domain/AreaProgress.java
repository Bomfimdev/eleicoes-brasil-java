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
@Table(name = "area_progress")
@IdClass(AreaProgressId.class)
public class AreaProgress {

  @Id
  @Column(name = "round_id", nullable = false)
  private UUID roundId;

  @Id
  @Column(name = "area_key", nullable = false)
  private String areaKey;

  @Column(name = "area_type", nullable = false)
  private String areaType;

  @Column(name = "state_code")
  private String stateCode;

  @Column(nullable = false)
  private String status;

  @Column(name = "counted_pct")
  private Double countedPct;

  private Long turnout;

  @Column(name = "totalized_at")
  private Instant totalizedAt;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private String progress;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
