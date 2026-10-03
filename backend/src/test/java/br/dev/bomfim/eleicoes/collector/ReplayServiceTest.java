package br.dev.bomfim.eleicoes.collector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.dev.bomfim.eleicoes.api.RealtimeVersionHub;
import br.dev.bomfim.eleicoes.config.EleicoesProperties;
import br.dev.bomfim.eleicoes.domain.AreaProgressRepository;
import br.dev.bomfim.eleicoes.domain.AreaResultRepository;
import br.dev.bomfim.eleicoes.domain.CollectorCycle;
import br.dev.bomfim.eleicoes.domain.ElectionRound;
import br.dev.bomfim.eleicoes.domain.ProgressSnapshotRepository;
import br.dev.bomfim.eleicoes.domain.ResultSnapshotRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReplayServiceTest {

  @Test
  void waitingWhenNoSnapshots() {
    EleicoesProperties props = new EleicoesProperties();
    props.getReplay().setSourceRoundSlug("demo-1");
    CollectorStore store = mock(CollectorStore.class);
    ElectionRound round = new ElectionRound();
    round.setId(UUID.randomUUID());
    round.setSlug("demo-1");
    when(store.findRound("demo-1")).thenReturn(Optional.of(round));
    when(store.startCycle(any(), any())).thenReturn(new CollectorCycle());

    ProgressSnapshotRepository progressSnaps = mock(ProgressSnapshotRepository.class);
    ResultSnapshotRepository resultSnaps = mock(ResultSnapshotRepository.class);
    when(progressSnaps.findByRoundIdOrderByCapturedAtAscIdAsc(any())).thenReturn(List.of());
    when(resultSnaps.findByRoundIdOrderByCapturedAtAscIdAsc(any())).thenReturn(List.of());

    ReplayService replay =
        new ReplayService(
            props,
            store,
            progressSnaps,
            resultSnaps,
            mock(AreaProgressRepository.class),
            mock(AreaResultRepository.class),
            mock(RealtimeVersionHub.class));

    assertThat(replay.runTick()).isEqualTo("waiting");
  }
}
