package br.dev.bomfim.eleicoes.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgressSnapshotRepository extends JpaRepository<ProgressSnapshot, Long> {

  List<ProgressSnapshot> findByRoundIdOrderByCapturedAtAscIdAsc(UUID roundId);

  List<ProgressSnapshot> findByRoundIdAndAreaKeyOrderByCapturedAtAscIdAsc(UUID roundId, String areaKey);

  List<ProgressSnapshot> findByRoundIdAndAreaTypeInAndCapturedAtLessThanEqualOrderByCapturedAtDescIdDesc(
      UUID roundId, Collection<String> areaTypes, Instant capturedAt);
}
