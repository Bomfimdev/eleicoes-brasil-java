package br.dev.bomfim.eleicoes.tse;

import br.dev.bomfim.eleicoes.config.EleicoesProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/** Lê fixtures TSE do classpath (modo DEVELOPMENT). Sem rede. */
public class ClasspathTseClient implements TseClient {

  private final String fixturesPath;

  public ClasspathTseClient(EleicoesProperties properties) {
    this.fixturesPath = trimSlash(properties.getTse().getFixturesPath());
  }

  public ClasspathTseClient(String fixturesPath) {
    this.fixturesPath = trimSlash(fixturesPath);
  }

  @Override
  public FetchResult get(String url) {
    long started = System.currentTimeMillis();
    String fileName = fileName(url);
    String path = fixturesPath + "/" + fileName;
    Resource resource = new ClassPathResource(path);
    if (!resource.exists()) {
      throw new NotFoundException(url);
    }
    try (InputStream in = resource.getInputStream()) {
      byte[] bytes = in.readAllBytes();
      String body = new String(bytes, StandardCharsets.UTF_8);
      return FetchResult.ok(body, null, sha256(bytes), System.currentTimeMillis() - started);
    } catch (IOException e) {
      throw new UnavailableException(e.getMessage(), url, null);
    }
  }

  static String fileName(String url) {
    int slash = url.lastIndexOf('/');
    String name = slash >= 0 ? url.substring(slash + 1) : url;
    int q = name.indexOf('?');
    return q >= 0 ? name.substring(0, q) : name;
  }

  private static String trimSlash(String path) {
    if (path == null || path.isBlank()) {
      return "fixtures/tse";
    }
    return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
  }

  private static String sha256(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
