package br.dev.bomfim.eleicoes.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ElectionRepository extends JpaRepository<Election, UUID> {

  Optional<Election> findBySlug(String slug);

  boolean existsBySlug(String slug);
}
