package br.dev.bomfim.eleicoes.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AreaProgressRepository extends JpaRepository<AreaProgress, AreaProgressId> {

  Optional<AreaProgress> findByRoundIdAndAreaKey(UUID roundId, String areaKey);

  List<AreaProgress> findByRoundIdAndAreaType(UUID roundId, String areaType);

  List<AreaProgress> findByRoundIdAndAreaTypeIn(UUID roundId, Collection<String> areaTypes);
}
