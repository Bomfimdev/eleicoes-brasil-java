package br.dev.bomfim.eleicoes.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AreaResultRepository extends JpaRepository<AreaResult, AreaResultId> {

  Optional<AreaResult> findByRoundIdAndOfficeIdAndAreaKey(UUID roundId, UUID officeId, String areaKey);

  List<AreaResult> findByRoundIdAndAreaKey(UUID roundId, String areaKey);

  List<AreaResult> findByRoundIdAndOfficeIdAndAreaType(UUID roundId, UUID officeId, String areaType);
}
