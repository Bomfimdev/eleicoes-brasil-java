package br.dev.bomfim.eleicoes.api;

import br.dev.bomfim.eleicoes.config.EleicoesProperties;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class RealtimeVersionHub {

  private static final Logger log = LoggerFactory.getLogger(RealtimeVersionHub.class);

  private final Map<String, AtomicLong> versions = new ConcurrentHashMap<>();
  private final Map<String, CopyOnWriteArrayList<Client>> clients = new ConcurrentHashMap<>();
  private final Map<String, AtomicLong> connectionsByIp = new ConcurrentHashMap<>();
  private final ResponseCache responseCache;
  private final EleicoesProperties properties;

  public RealtimeVersionHub(ResponseCache responseCache, EleicoesProperties properties) {
    this.responseCache = responseCache;
    this.properties = properties;
  }

  public long currentVersion(String roundSlug) {
    return versions.computeIfAbsent(roundSlug, k -> new AtomicLong(0)).get();
  }

  public long publish(String roundSlug) {
    long v = versions.computeIfAbsent(roundSlug, k -> new AtomicLong(0)).incrementAndGet();
    responseCache.invalidateRound(roundSlug);
    String payload =
        "{\"electionId\":\""
            + roundSlug
            + "\",\"version\":"
            + v
            + ",\"timestamp\":\""
            + Instant.now()
            + "\"}";
    List<Client> list = clients.getOrDefault(roundSlug, new CopyOnWriteArrayList<>());
    for (Client c : list) {
      try {
        c.emitter().send(SseEmitter.event().name("version").data(payload));
      } catch (IOException | IllegalStateException ex) {
        remove(roundSlug, c);
      }
    }
    log.debug("Realtime version {} for {}", v, roundSlug);
    return v;
  }

  public boolean add(String roundSlug, String ip, SseEmitter emitter) {
    int max = Math.max(1, properties.getSse().getMaxConnectionsPerIp());
    AtomicLong count = connectionsByIp.computeIfAbsent(ip, k -> new AtomicLong(0));
    while (true) {
      long cur = count.get();
      if (cur >= max) {
        return false;
      }
      if (count.compareAndSet(cur, cur + 1)) {
        break;
      }
    }
    Client client = new Client(ip, emitter);
    clients.computeIfAbsent(roundSlug, k -> new CopyOnWriteArrayList<>()).add(client);
    emitter.onCompletion(() -> remove(roundSlug, client));
    emitter.onTimeout(() -> remove(roundSlug, client));
    emitter.onError(e -> remove(roundSlug, client));
    return true;
  }

  public void remove(String roundSlug, Client client) {
    CopyOnWriteArrayList<Client> list = clients.get(roundSlug);
    if (list != null) {
      list.remove(client);
    }
    AtomicLong count = connectionsByIp.get(client.ip());
    if (count != null) {
      count.updateAndGet(v -> Math.max(0, v - 1));
    }
    try {
      client.emitter().complete();
    } catch (Exception ignored) {
      // already closed
    }
  }

  /** Exposed for tests. */
  int connectionsForIp(String ip) {
    AtomicLong c = connectionsByIp.get(ip);
    return c == null ? 0 : (int) c.get();
  }

  List<String> activeRounds() {
    return new ArrayList<>(clients.keySet());
  }

  public record Client(String ip, SseEmitter emitter) {}
}
