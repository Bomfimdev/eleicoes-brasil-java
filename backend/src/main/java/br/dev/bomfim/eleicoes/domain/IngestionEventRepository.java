package br.dev.bomfim.eleicoes.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IngestionEventRepository extends JpaRepository<IngestionEvent, Long> {}
