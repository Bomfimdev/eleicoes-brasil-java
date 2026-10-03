package br.dev.bomfim.eleicoes.api;

import static org.assertj.core.api.Assertions.assertThat;

import br.dev.bomfim.eleicoes.config.EleicoesProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.json.JsonMapper;

class RealtimeVersionHubTest {

  private RealtimeVersionHub hub;

  @BeforeEach
  void setUp() {
    EleicoesProperties props = new EleicoesProperties();
    props.getSse().setMaxConnectionsPerIp(2);
    props.getApi().setCacheTtlSeconds(15);
    ResponseCache cache = new ResponseCache(props, JsonMapper.builder().findAndAddModules().build());
    hub = new RealtimeVersionHub(cache, props);
  }

  @Test
  void publishIncrementsVersion() {
    assertThat(hub.currentVersion("demo-1")).isZero();
    assertThat(hub.publish("demo-1")).isEqualTo(1);
    assertThat(hub.publish("demo-1")).isEqualTo(2);
  }

  @Test
  void rejectsWhenIpExceedsLimit() {
    assertThat(hub.add("demo-1", "1.1.1.1", new SseEmitter())).isTrue();
    assertThat(hub.add("demo-1", "1.1.1.1", new SseEmitter())).isTrue();
    assertThat(hub.add("demo-1", "1.1.1.1", new SseEmitter())).isFalse();
    assertThat(hub.connectionsForIp("1.1.1.1")).isEqualTo(2);
  }
}
