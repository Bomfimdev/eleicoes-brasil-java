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
@Table(name = "collector_cycles")
public class CollectorCycle {

  @Id
  private UUID id;

  @Column(name = "round_id", nullable = false)
  private UUID roundId;

  @Column(nullable = false)
  private String mode;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "finished_at")
  private Instant finishedAt;

  @Column(nullable = false)
  private int requests;

  @Column(nullable = false)
  private int ok;

  @Column(name = "not_modified", nullable = false)
  private int notModified;

  @Column(name = "not_found", nullable = false)
  private int notFound;

  @Column(nullable = false)
  private int errors;

  @Column(name = "avg_latency_ms")
  private Double avgLatencyMs;

  @Column(name = "p95_latency_ms")
  private Double p95LatencyMs;

  @Column(nullable = false)
  private String status;

  private String error;
}
