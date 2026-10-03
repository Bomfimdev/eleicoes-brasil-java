package br.dev.bomfim.eleicoes.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "eleicoes")
public class EleicoesProperties {

  private String appMode = "DEVELOPMENT";
  private String corsOrigins = "http://localhost:4200";
  private String electionWindows = "";
  private boolean collectCityResults = false;
  private final Collector collector = new Collector();
  private final Tse tse = new Tse();
  private final Api api = new Api();
  private final Sse sse = new Sse();
  private final Replay replay = new Replay();

  public String getAppMode() {
    return appMode;
  }

  public void setAppMode(String appMode) {
    this.appMode = appMode;
  }

  public String getCorsOrigins() {
    return corsOrigins;
  }

  public void setCorsOrigins(String corsOrigins) {
    this.corsOrigins = corsOrigins;
  }

  public String getElectionWindows() {
    return electionWindows;
  }

  public void setElectionWindows(String electionWindows) {
    this.electionWindows = electionWindows;
  }

  public boolean isCollectCityResults() {
    return collectCityResults;
  }

  public void setCollectCityResults(boolean collectCityResults) {
    this.collectCityResults = collectCityResults;
  }

  public Collector getCollector() {
    return collector;
  }

  public Tse getTse() {
    return tse;
  }

  public Api getApi() {
    return api;
  }

  public Sse getSse() {
    return sse;
  }

  public Replay getReplay() {
    return replay;
  }

  public static class Replay {
    /** Round cujos snapshots serão reproduzidos. */
    private String sourceRoundSlug = "demo-1";
    /** Multiplicador de velocidade (só informativo no intervalo do scheduler). */
    private double speed = 10.0;

    public String getSourceRoundSlug() {
      return sourceRoundSlug;
    }

    public void setSourceRoundSlug(String sourceRoundSlug) {
      this.sourceRoundSlug = sourceRoundSlug;
    }

    public double getSpeed() {
      return speed;
    }

    public void setSpeed(double speed) {
      this.speed = speed;
    }
  }

  public static class Api {
    private int cacheTtlSeconds = 15;

    public int getCacheTtlSeconds() {
      return cacheTtlSeconds;
    }

    public void setCacheTtlSeconds(int cacheTtlSeconds) {
      this.cacheTtlSeconds = cacheTtlSeconds;
    }
  }

  public static class Sse {
    private int maxConnectionsPerIp = 5;

    public int getMaxConnectionsPerIp() {
      return maxConnectionsPerIp;
    }

    public void setMaxConnectionsPerIp(int maxConnectionsPerIp) {
      this.maxConnectionsPerIp = maxConnectionsPerIp;
    }
  }

  public static class Collector {
    private boolean enabled = true;
    /** Só para desenvolvimento local: ignora ELECTION_WINDOWS. */
    private boolean ignoreWindows = false;
    private String roundSlug = "demo-1";

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public boolean isIgnoreWindows() {
      return ignoreWindows;
    }

    public void setIgnoreWindows(boolean ignoreWindows) {
      this.ignoreWindows = ignoreWindows;
    }

    public String getRoundSlug() {
      return roundSlug;
    }

    public void setRoundSlug(String roundSlug) {
      this.roundSlug = roundSlug;
    }
  }

  public static class Tse {
    private String baseUrl = "";
    private String environment = "";
    private String providerRoundId = "";
    private long pollIntervalMs = 15_000L;
    private double requestsPerSecond = 20.0;
    private String fixturesPath = "fixtures/tse";

    public String getBaseUrl() {
      return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
    }

    public String getEnvironment() {
      return environment;
    }

    public void setEnvironment(String environment) {
      this.environment = environment;
    }

    public String getProviderRoundId() {
      return providerRoundId;
    }

    public void setProviderRoundId(String providerRoundId) {
      this.providerRoundId = providerRoundId;
    }

    public long getPollIntervalMs() {
      return pollIntervalMs;
    }

    public void setPollIntervalMs(long pollIntervalMs) {
      this.pollIntervalMs = pollIntervalMs;
    }

    public double getRequestsPerSecond() {
      return requestsPerSecond;
    }

    public void setRequestsPerSecond(double requestsPerSecond) {
      this.requestsPerSecond = requestsPerSecond;
    }

    public String getFixturesPath() {
      return fixturesPath;
    }

    public void setFixturesPath(String fixturesPath) {
      this.fixturesPath = fixturesPath;
    }
  }
}
