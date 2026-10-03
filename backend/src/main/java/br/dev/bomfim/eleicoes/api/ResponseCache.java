package br.dev.bomfim.eleicoes.api;

import br.dev.bomfim.eleicoes.config.EleicoesProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ResponseCache {

  private final Map<String, Entry> entries = new ConcurrentHashMap<>();
  private final EleicoesProperties properties;
  private final JsonMapper mapper;

  public ResponseCache(EleicoesProperties properties, JsonMapper mapper) {
    this.properties = properties;
    this.mapper = mapper;
  }

  public Entry get(String roundSlug, String resourceKey) {
    String key = roundSlug + "|" + resourceKey;
    Entry e = entries.get(key);
    if (e == null) {
      return null;
    }
    if (Instant.now().isAfter(e.expiresAt())) {
      entries.remove(key);
      return null;
    }
    return e;
  }

  public Entry put(String roundSlug, String resourceKey, Object body) {
    try {
      String json = mapper.writeValueAsString(body);
      String etag = "\"" + sha256(json) + "\"";
      Instant expires =
          Instant.now().plusSeconds(Math.max(1, properties.getApi().getCacheTtlSeconds()));
      Entry entry = new Entry(json, etag, expires);
      entries.put(roundSlug + "|" + resourceKey, entry);
      return entry;
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  public void invalidateRound(String roundSlug) {
    String prefix = roundSlug + "|";
    Iterator<Map.Entry<String, Entry>> it = entries.entrySet().iterator();
    while (it.hasNext()) {
      if (it.next().getKey().startsWith(prefix)) {
        it.remove();
      }
    }
  }

  private static String sha256(String json) {
    try {
      return HexFormat.of()
          .formatHex(MessageDigest.getInstance("SHA-256").digest(json.getBytes(StandardCharsets.UTF_8)))
          .substring(0, 16);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  public record Entry(String json, String etag, Instant expiresAt) {}
}
