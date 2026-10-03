package br.dev.bomfim.eleicoes.collector;

import br.dev.bomfim.eleicoes.config.EleicoesProperties;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ElectionWindowGate {

  private final EleicoesProperties properties;
  private final List<Window> windows;

  public ElectionWindowGate(EleicoesProperties properties) {
    this.properties = properties;
    this.windows = parse(properties.getElectionWindows());
  }

  public boolean isOpen() {
    if (properties.getCollector().isIgnoreWindows()) {
      return true;
    }
    if (windows.isEmpty()) {
      return false;
    }
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.systemDefault());
    for (Window w : windows) {
      if (!now.isBefore(w.start()) && !now.isAfter(w.end())) {
        return true;
      }
    }
    return false;
  }

  static List<Window> parse(String raw) {
    List<Window> out = new ArrayList<>();
    if (raw == null || raw.isBlank()) {
      return out;
    }
    for (String part : raw.split(",")) {
      String trimmed = part.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      int slash = trimmed.indexOf('/');
      if (slash <= 0 || slash >= trimmed.length() - 1) {
        throw new IllegalArgumentException("Janela inválida (use inicio/fim): " + trimmed);
      }
      OffsetDateTime start = OffsetDateTime.parse(trimmed.substring(0, slash).trim());
      OffsetDateTime end = OffsetDateTime.parse(trimmed.substring(slash + 1).trim());
      out.add(new Window(start, end));
    }
    return out;
  }

  public record Window(OffsetDateTime start, OffsetDateTime end) {}
}
