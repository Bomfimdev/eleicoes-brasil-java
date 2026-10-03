package br.dev.bomfim.eleicoes.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngestionEventRepository extends JpaRepository<IngestionEvent, Long> {

  List<IngestionEvent> findByRoundIdOrderByIdDesc(UUID roundId, Pageable pageable);

  List<IngestionEvent> findByRoundIdAndOccurredAtAfter(UUID roundId, Instant after);
}
