package br.dev.bomfim.eleicoes.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectorCycleRepository extends JpaRepository<CollectorCycle, UUID> {}
