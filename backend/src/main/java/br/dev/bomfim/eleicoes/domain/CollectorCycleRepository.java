package br.dev.bomfim.eleicoes.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectorCycleRepository extends JpaRepository<CollectorCycle, UUID> {

  Optional<CollectorCycle> findFirstByRoundIdAndStatusNotOrderByStartedAtDesc(UUID roundId, String status);

  Optional<CollectorCycle> findFirstByRoundIdAndStatusInOrderByStartedAtDesc(
      UUID roundId, Collection<String> statuses);

  List<CollectorCycle> findByRoundIdOrderByStartedAtDesc(UUID roundId, Pageable pageable);

  List<CollectorCycle> findByRoundIdAndStartedAtAfter(UUID roundId, Instant after);
}