package br.dev.bomfim.eleicoes.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartyRepository extends JpaRepository<Party, PartyId> {

  List<Party> findByRoundIdOrderByNumberAsc(UUID roundId);
}
