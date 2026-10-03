package br.dev.bomfim.eleicoes.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectorCycleRepository extends JpaRepository<CollectorCycle, UUID> {

  Optional<CollectorCycle> findFirstByRoundIdAndStatusNotOrderByStartedAtDesc(UUID roundId, String status);

  Optional<CollectorCycle> findFirstByRoundIdAndStatusInOrderByStartedAtDesc(
      UUID roundId, java.util.Collection<String> statuses);
}
