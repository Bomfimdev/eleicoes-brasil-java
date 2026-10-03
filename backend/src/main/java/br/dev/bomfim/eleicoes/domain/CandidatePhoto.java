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

@Getter
@Setter
@Entity
@Table(name = "candidate_photos")
@IdClass(CandidatePhotoId.class)
public class CandidatePhoto {

  @Id
  @Column(name = "round_id", nullable = false)
  private UUID roundId;

  @Id
  @Column(name = "candidate_key", nullable = false)
  private String candidateKey;

  @Column(name = "content_type")
  private String contentType;

  /** URL externa; bytes da foto não ficam no banco. */
  @Column(name = "photo_url")
  private String photoUrl;

  @Column(name = "fetched_at", nullable = false)
  private Instant fetchedAt;
}
