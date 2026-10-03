package br.dev.bomfim.eleicoes.api;

import br.dev.bomfim.eleicoes.config.EleicoesProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Eleições", description = "API read-only de apuração")
public class ElectionController {

  private final ElectionQueryService queries;
  private final ResponseCache cache;
  private final EleicoesProperties properties;

  public ElectionController(
      ElectionQueryService queries, ResponseCache cache, EleicoesProperties properties) {
    this.queries = queries;
    this.cache = cache;
    this.properties = properties;
  }

  @GetMapping("/meta")
  @Operation(summary = "Metadados da API")
  public Map<String, Object> meta() {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("app", Map.of("name", "eleicoes-brasil-api", "mode", properties.getAppMode()));
    body.put("timezone", "America/Sao_Paulo");
    body.put("source", "Tribunal Superior Eleitoral — TSE");
    body.put("disclaimer", "Projeto independente — não é serviço oficial da Justiça Eleitoral");
    return body;
  }

  @GetMapping("/elections")
  @Operation(summary = "Lista eleições e turnos")
  public ResponseEntity<String> elections(HttpServletRequest request) {
    return cached("*", "elections", request, queries::listElections);
  }

  @GetMapping("/elections/{id}")
  @Operation(summary = "Detalhe do turno (slug)")
  public ResponseEntity<String> round(@PathVariable("id") String id, HttpServletRequest request) {
    return cached(id, "round", request, () -> queries.getRound(id));
  }

  @GetMapping("/elections/{id}/overview")
  @Operation(summary = "Overview nacional")
  public ResponseEntity<String> overview(@PathVariable("id") String id, HttpServletRequest request) {
    return cached(id, "overview", request, () -> queries.overview(id));
  }

  @GetMapping("/elections/{id}/results")
  @Operation(summary = "Resultado por cargo e área")
  public ResponseEntity<String> results(
      @PathVariable("id") String id,
      @RequestParam(value = "office", required = false) String office,
      @RequestParam(value = "area", required = false, defaultValue = "br") String area,
      HttpServletRequest request) {
    return cached(
        id, "results:" + office + ":" + area, request, () -> queries.result(id, office, area));
  }

  @GetMapping("/elections/{id}/states")
  @Operation(summary = "Lista UFs com progresso")
  public ResponseEntity<String> states(@PathVariable("id") String id, HttpServletRequest request) {
    return cached(id, "states", request, () -> queries.states(id));
  }

  @GetMapping("/elections/{id}/states/{uf}")
  @Operation(summary = "Detalhe da UF")
  public ResponseEntity<String> state(
      @PathVariable("id") String id, @PathVariable("uf") String uf, HttpServletRequest request) {
    return cached(id, "state:" + uf, request, () -> queries.state(id, uf));
  }

  @GetMapping("/elections/{id}/export")
  @Operation(summary = "Export do overview para arquivo estático (modo arquivado)")
  public ResponseEntity<String> export(@PathVariable("id") String id) {
    // Sem cache curto: snapshot completo para gravar no Pages
    Object body = queries.overview(id);
    ResponseCache.Entry entry = cache.put(id, "export", body);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + id + ".json\"")
        .body(entry.json());
  }

  @GetMapping("/elections/{id}/states/{uf}/results")
  @Operation(summary = "Resultado da UF")
  public ResponseEntity<String> stateResults(
      @PathVariable("id") String id,
      @PathVariable("uf") String uf,
      @RequestParam(value = "office", required = false) String office,
      HttpServletRequest request) {
    String area = uf.toLowerCase();
    return cached(
        id,
        "results:" + office + ":" + area,
        request,
        () -> queries.result(id, office, area));
  }

  @GetMapping("/elections/{id}/operations")
  @Operation(summary = "Painel ao vivo (coleta e ritmo)")
  public ResponseEntity<String> operations(
      @PathVariable("id") String id, HttpServletRequest request) {
    return cached(id, "operations", request, () -> queries.operations(id));
  }

  @GetMapping("/elections/{id}/events")
  @Operation(summary = "Eventos recentes de ingestão")
  public ResponseEntity<String> events(
      @PathVariable("id") String id,
      @RequestParam(value = "limit", required = false, defaultValue = "30") int limit,
      HttpServletRequest request) {
    return cached(id, "events:" + limit, request, () -> queries.events(id, limit));
  }

  @GetMapping("/elections/{id}/timeline")
  @Operation(summary = "Histórico da apuração nacional (ou snapshot em um instante)")
  public ResponseEntity<String> timeline(
      @PathVariable("id") String id,
      @RequestParam(value = "at", required = false) String at,
      HttpServletRequest request) {
    if (at != null && !at.isBlank()) {
      return cached(id, "timeline-at:" + at, request, () -> queries.timelineAt(id, at));
    }
    return cached(id, "timeline", request, () -> queries.timeline(id));
  }

  @GetMapping("/elections/{id}/states/{uf}/cities")
  @Operation(summary = "Lista municípios da UF (capitais e com progresso)")
  public ResponseEntity<String> cities(
      @PathVariable("id") String id,
      @PathVariable("uf") String uf,
      @RequestParam(value = "q", required = false) String q,
      @RequestParam(value = "page", required = false, defaultValue = "0") int page,
      @RequestParam(value = "pageSize", required = false, defaultValue = "40") int pageSize,
      HttpServletRequest request) {
    return cached(
        id,
        "cities:" + uf + ":" + q + ":" + page + ":" + pageSize,
        request,
        () -> queries.cities(id, uf, q, page, pageSize));
  }

  @GetMapping("/elections/{id}/states/{uf}/cities/{city}")
  @Operation(summary = "Detalhe do município")
  public ResponseEntity<String> city(
      @PathVariable("id") String id,
      @PathVariable("uf") String uf,
      @PathVariable("city") String city,
      HttpServletRequest request) {
    return cached(id, "city:" + uf + ":" + city, request, () -> queries.city(id, uf, city));
  }

  @GetMapping("/elections/{id}/series")
  @Operation(summary = "Série temporal de percentuais por candidato")
  public ResponseEntity<String> series(
      @PathVariable("id") String id,
      @RequestParam("office") String office,
      @RequestParam(value = "area", required = false, defaultValue = "br") String area,
      HttpServletRequest request) {
    return cached(
        id, "series:" + office + ":" + area, request, () -> queries.series(id, office, area));
  }

  @GetMapping("/elections/{id}/compare")
  @Operation(summary = "Comparar estados lado a lado")
  public ResponseEntity<String> compare(
      @PathVariable("id") String id,
      @RequestParam("states") String states,
      @RequestParam(value = "office", required = false) String office,
      HttpServletRequest request) {
    java.util.List<String> ufs =
        java.util.Arrays.stream(states.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    return cached(
        id,
        "compare:" + String.join(",", ufs) + ":" + office,
        request,
        () -> queries.compare(id, ufs, office));
  }

  private ResponseEntity<String> cached(
      String roundSlug, String resourceKey, HttpServletRequest request, Supplier<Object> loader) {
    ResponseCache.Entry entry = cache.get(roundSlug, resourceKey);
    if (entry == null) {
      Object body = loader.get();
      entry = cache.put(roundSlug, resourceKey, body);
    }
    String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
    if (ifNoneMatch != null && ifNoneMatch.equals(entry.etag())) {
      return ResponseEntity.status(304)
          .header(HttpHeaders.ETAG, entry.etag())
          .header(HttpHeaders.CACHE_CONTROL, liveCacheControl())
          .build();
    }
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .header(HttpHeaders.ETAG, entry.etag())
        .header(HttpHeaders.CACHE_CONTROL, liveCacheControl())
        .body(entry.json());
  }

  private String liveCacheControl() {
    int ttl = Math.max(1, properties.getApi().getCacheTtlSeconds());
    return "public, max-age=" + ttl;
  }
}
