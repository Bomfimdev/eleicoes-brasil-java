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
@Table(name = "ingestion_events")
public class IngestionEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "round_id", nullable = false)
  private UUID roundId;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(nullable = false)
  private String type;

  @Column(name = "area_key")
  private String areaKey;

  @Column(name = "state_code")
  private String stateCode;

  @Column(name = "sections_added")
  private Integer sectionsAdded;

  @Column(name = "votes_added")
  private Long votesAdded;

  @Column(name = "counted_pct")
  private Double countedPct;

  private String message;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private String context;
}
