package br.dev.bomfim.eleicoes.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficeRepository extends JpaRepository<Office, UUID> {

  List<Office> findByRoundIdOrderBySlugAsc(UUID roundId);

  Optional<Office> findByRoundIdAndSlug(UUID roundId, String slug);
}
