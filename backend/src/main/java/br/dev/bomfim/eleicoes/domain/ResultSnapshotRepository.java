package br.dev.bomfim.eleicoes.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResultSnapshotRepository extends JpaRepository<ResultSnapshot, Long> {

  List<ResultSnapshot> findByRoundIdOrderByCapturedAtAscIdAsc(UUID roundId);

  List<ResultSnapshot>
      findByRoundIdAndOfficeIdAndAreaTypeInAndCapturedAtLessThanEqualOrderByCapturedAtDescIdDesc(
          UUID roundId, UUID officeId, Collection<String> areaTypes, Instant capturedAt);

  List<ResultSnapshot> findByRoundIdAndOfficeIdAndAreaKeyAndCandidatesIsNotNullOrderByCapturedAtAscIdAsc(
      UUID roundId, UUID officeId, String areaKey);
}
