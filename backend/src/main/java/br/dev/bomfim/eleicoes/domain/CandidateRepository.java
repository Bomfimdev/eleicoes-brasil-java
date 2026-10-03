package br.dev.bomfim.eleicoes.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateRepository extends JpaRepository<Candidate, UUID> {

  List<Candidate> findByRoundIdAndOfficeIdOrderByNumberAsc(UUID roundId, UUID officeId);
}
