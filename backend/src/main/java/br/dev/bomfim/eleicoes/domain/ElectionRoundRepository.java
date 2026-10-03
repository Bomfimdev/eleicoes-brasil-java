package br.dev.bomfim.eleicoes.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ElectionRoundRepository extends JpaRepository<ElectionRound, UUID> {

  Optional<ElectionRound> findBySlug(String slug);

  List<ElectionRound> findByElectionIdOrderByRoundAsc(UUID electionId);
}
