package br.dev.bomfim.eleicoes.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/realtime")
@Tag(name = "Realtime", description = "SSE — apenas evento de versão")
public class RealtimeController {

  private static final long SSE_TIMEOUT_MS = 0L; // sem timeout do servidor

  private final ElectionQueryService queries;
  private final RealtimeVersionHub hub;

  public RealtimeController(ElectionQueryService queries, RealtimeVersionHub hub) {
    this.queries = queries;
    this.hub = hub;
  }

  @GetMapping(value = "/elections/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @Operation(summary = "Stream SSE de versão do turno")
  public SseEmitter stream(@PathVariable("id") String id, HttpServletRequest request)
      throws IOException {
    queries.getRound(id); // 404 se não existir
    String ip = clientIp(request);
    SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
    if (!hub.add(id, ip, emitter)) {
      emitter.send(SseEmitter.event().reconnectTime(30_000));
      emitter.complete();
      return emitter;
    }
    long version = hub.currentVersion(id);
    String ready =
        "{\"electionId\":\""
            + id
            + "\",\"version\":"
            + version
            + ",\"timestamp\":\""
            + Instant.now()
            + "\"}";
    emitter.send(SseEmitter.event().reconnectTime(5_000).name("ready").data(ready));
    return emitter;
  }

  static String clientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
  }
}
