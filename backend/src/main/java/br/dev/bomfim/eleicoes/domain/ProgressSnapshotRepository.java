package br.dev.bomfim.eleicoes.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgressSnapshotRepository extends JpaRepository<ProgressSnapshot, Long> {

  List<ProgressSnapshot> findByRoundIdOrderByCapturedAtAscIdAsc(UUID roundId);
}
