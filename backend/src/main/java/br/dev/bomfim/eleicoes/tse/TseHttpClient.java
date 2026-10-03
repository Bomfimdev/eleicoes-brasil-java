package br.dev.bomfim.eleicoes.tse;

import br.dev.bomfim.eleicoes.config.EleicoesProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cliente HTTP para SIMULATION/PRODUCTION: rate limit global + If-None-Match (304).
 */
public class TseHttpClient implements TseClient {

  private static final Logger log = LoggerFactory.getLogger(TseHttpClient.class);

  private final HttpClient http;
  private final double requestsPerSecond;
  private final Duration timeout;
  private final Map<String, String> etags = new ConcurrentHashMap<>();
  private long nextSlotMs;

  public TseHttpClient(EleicoesProperties properties) {
    this.requestsPerSecond = Math.max(0.1, properties.getTse().getRequestsPerSecond());
    this.timeout = Duration.ofSeconds(30);
    this.http = HttpClient.newBuilder().connectTimeout(timeout).followRedirects(HttpClient.Redirect.NORMAL).build();
  }

  /** Construtor de teste. */
  public TseHttpClient(double requestsPerSecond, HttpClient http) {
    this.requestsPerSecond = Math.max(0.1, requestsPerSecond);
    this.timeout = Duration.ofSeconds(30);
    this.http = http;
  }

  @Override
  public synchronized void resetValidators() {
    etags.clear();
  }

  @Override
  public FetchResult get(String url) {
    acquireSlot();
    long started = System.currentTimeMillis();
    try {
      HttpRequest.Builder builder =
          HttpRequest.newBuilder(URI.create(url))
              .timeout(timeout)
              .header("User-Agent", "eleicoes-brasil-collector/1.0")
              .header("Accept", "application/json")
              .GET();
      String etag = etags.get(url);
      if (etag != null) {
        builder.header("If-None-Match", etag);
      }
      HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      long duration = System.currentTimeMillis() - started;
      int status = response.statusCode();
      if (status == 304) {
        return FetchResult.notModified(duration);
      }
      if (status == 404) {
        throw new NotFoundException(url);
      }
      if (status == 403 || status == 429) {
        throw new UnavailableException("blocked by source HTTP " + status, url, status);
      }
      if (status < 200 || status >= 300) {
        throw new UnavailableException("HTTP " + status, url, status);
      }
      String body = response.body();
      String newEtag = response.headers().firstValue("etag").orElse(null);
      if (newEtag != null) {
        etags.put(url, newEtag);
      }
      return FetchResult.ok(body, newEtag, sha256(body), duration);
    } catch (NotFoundException | UnavailableException e) {
      throw e;
    } catch (Exception e) {
      log.warn("Falha HTTP TSE: {} — {}", url, e.getMessage());
      throw new UnavailableException(e.getMessage(), url, null);
    }
  }

  private synchronized void acquireSlot() {
    long now = System.currentTimeMillis();
    long interval = Math.round(1000.0 / requestsPerSecond);
    long slot = Math.max(now, nextSlotMs);
    nextSlotMs = slot + interval;
    long wait = slot - now;
    if (wait > 0) {
      try {
        Thread.sleep(wait);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new UnavailableException("interrupted", "rate-limit", null);
      }
    }
  }

  private static String sha256(String body) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
