package br.dev.bomfim.eleicoes.tse;

import br.dev.bomfim.eleicoes.tse.model.AreaProgressView;
import br.dev.bomfim.eleicoes.tse.model.AreaResultView;
import br.dev.bomfim.eleicoes.tse.model.CandidateResult;
import br.dev.bomfim.eleicoes.tse.model.CountryProgress;
import br.dev.bomfim.eleicoes.tse.model.CountingProgress;
import br.dev.bomfim.eleicoes.tse.model.ElectionConfig;
import br.dev.bomfim.eleicoes.tse.model.Fetched;
import br.dev.bomfim.eleicoes.tse.model.PartyResult;
import br.dev.bomfim.eleicoes.tse.model.Provenance;
import br.dev.bomfim.eleicoes.tse.model.StateProgress;
import br.dev.bomfim.eleicoes.tse.model.TseOffice;
import br.dev.bomfim.eleicoes.tse.model.VotesSummary;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Único módulo que conhece campos brutos do TSE (cdabr, pvapn, etc.). Espelha o adapter-2026 do
 * projeto original sem portar Node/Zod.
 */
public class TseAdapter2026 {

  public static final String ID = "tse-2026";
  public static final String VERSION = "2026-v1";

  private static final Set<String> STATES =
      Set.of(
          "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA", "PB",
          "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO");

  private static final Map<String, String[]> KNOWN_OFFICES =
      Map.of(
          "1", new String[] {"presidente", "majoritarian", "country"},
          "3", new String[] {"governador", "majoritarian", "state"},
          "5", new String[] {"senador", "majoritarian", "state"},
          "6", new String[] {"deputado-federal", "proportional", "state"},
          "7", new String[] {"deputado-estadual", "proportional", "state"},
          "8", new String[] {"deputado-distrital", "proportional", "state"});

  private static final Map<String, String> DEFAULT_DIRS =
      Map.of(
          "u", "<base>/<ambiente>/<ciclo>/<cd_eleicao>/dados/<uf>",
          "ab", "<base>/<ambiente>/<ciclo>/<cd_eleicao>/dados/<uf>",
          "cm", "<base>/<ambiente>/<ciclo>/<cd_eleicao>/config");

  private static final Map<String, String> STATUS =
      Map.of("n", "not-started", "p", "in-progress", "f", "finished");

  private final TseClient client;
  private final JsonMapper mapper;
  private final String baseUrl;
  private final String environment;
  private final String providerRoundId;
  private final int roundNumber;
  private final String roundDate;

  private Context context;

  public TseAdapter2026(
      TseClient client,
      JsonMapper mapper,
      String baseUrl,
      String environment,
      String providerRoundId,
      int roundNumber,
      String roundDate) {
    this.client = client;
    this.mapper = mapper;
    this.baseUrl = trimSlash(baseUrl);
    this.environment = environment;
    this.providerRoundId = providerRoundId;
    this.roundNumber = roundNumber;
    this.roundDate = roundDate;
  }

  public ElectionConfig getElectionConfig() {
    String url = baseUrl + "/" + environment + "/comum/config/ele-c.json";
    TseClient.FetchResult res = client.get(url);
    if (res.notModified() && context != null) {
      return context.config;
    }
    if (res.notModified()) {
      throw new TsePayloadException("unexpected 304 for first config fetch", url);
    }
    JsonNode file = parse(res.body(), url);
    validateElectionConfig(file, url);
    JsonNode pleito = pickPleito(file, url);
    Map<String, String> dirs = new HashMap<>();
    if (file.path("arq").isArray()) {
      for (JsonNode a : file.get("arq")) {
        dirs.put(a.path("tp").asText(), a.path("dir").asText());
      }
    }
    String cycle = text(pleito, "c");
    if (cycle == null || cycle.isBlank()) {
      cycle = "ele" + (roundDate != null && roundDate.length() >= 4 ? roundDate.substring(0, 4) : "2026");
    }

    List<TseOffice> offices = new ArrayList<>();
    Map<String, TseOffice> byKey = new HashMap<>();
    for (JsonNode election : pleito.path("e")) {
      for (JsonNode abr : election.path("abr")) {
        for (JsonNode cp : abr.path("cp")) {
          if ("3".equals(text(cp, "tp"))) {
            continue;
          }
          String code = TseNumbers.asCode(scalar(cp, "cd"));
          String eleCode = TseNumbers.asCode(scalar(election, "cd"));
          String key = code + "|" + eleCode;
          if (byKey.containsKey(key)) {
            continue;
          }
          TseOffice office = toOffice(cp, election);
          byKey.put(key, office);
          offices.add(office);
        }
      }
    }

    JsonNode progressElection = null;
    for (JsonNode e : pleito.path("e")) {
      if ("8".equals(TseNumbers.asCode(scalar(e, "tp")))) {
        progressElection = e;
        break;
      }
    }
    if (progressElection == null) {
      for (JsonNode e : pleito.path("e")) {
        String tp = TseNumbers.asCode(scalar(e, "tp"));
        if ("1".equals(tp) || "3".equals(tp)) {
          progressElection = e;
          break;
        }
      }
    }
    if (progressElection == null && pleito.path("e").isArray() && !pleito.path("e").isEmpty()) {
      progressElection = pleito.path("e").get(0);
    }
    if (progressElection == null) {
      throw new TsePayloadException("pleito without elections", url);
    }

    List<String> eleCodes = new ArrayList<>();
    for (JsonNode e : pleito.path("e")) {
      eleCodes.add(TseNumbers.asCode(scalar(e, "cd")));
    }

    ElectionConfig config =
        new ElectionConfig(
            TseNumbers.asCode(scalar(pleito, "cd")),
            TseTime.isoDate(text(pleito, "dt")),
            roundNumber,
            TseNumbers.asCode(scalar(progressElection, "cd")),
            List.copyOf(eleCodes),
            List.copyOf(offices));
    context = new Context(cycle, TseNumbers.asCode(scalar(pleito, "cd")), dirs, config);
    return config;
  }

  public Fetched<CountryProgress> getCountryProgress(String electionCode) {
    Context ctx = ctx();
    String url =
        dir(ctx, "ab", electionCode, "br") + "/br-e" + pad(electionCode, 6) + "-ab.json";
    TseClient.FetchResult res = client.get(url);
    if (res.notModified()) {
      return Fetched.unchanged();
    }
    JsonNode file = parse(res.body(), url);
    validateProgressFile(file, url);
    CountingProgress country = null;
    List<AreaProgressView> states = new ArrayList<>();
    for (JsonNode entry : file.path("abr")) {
      String tp = text(entry, "tpabr");
      if ("br".equalsIgnoreCase(tp)) {
        country = toProgress(entry);
      } else if ("uf".equalsIgnoreCase(tp)) {
        String uf = TseNumbers.asCode(scalar(entry, "cdabr")).toUpperCase(Locale.ROOT);
        if (STATES.contains(uf)) {
          states.add(new AreaProgressView(uf.toLowerCase(Locale.ROOT), "state", uf, toProgress(entry)));
        }
      }
    }
    if (country == null) {
      throw new TsePayloadException("EA14 without a br entry", url);
    }
    return Fetched.of(
        new CountryProgress(country, states), provenance(url, file, res));
  }

  public Fetched<AreaResultView> getCountryResult(TseOffice office) {
    return getResult(office, "br", "country", null, null);
  }

  public Fetched<AreaResultView> getStateResult(TseOffice office, String uf) {
    String code = uf.toUpperCase(Locale.ROOT);
    return getResult(office, code.toLowerCase(Locale.ROOT), "state", code, null);
  }

  public Fetched<AreaResultView> getCityResult(TseOffice office, String uf, String cityCode) {
    String code = uf.toUpperCase(Locale.ROOT);
    String city = pad(cityCode, 5);
    return getResult(office, code.toLowerCase(Locale.ROOT) + "-" + city, "city", code, city);
  }

  public Fetched<StateProgress> getStateProgress(String electionCode, String uf) {
    Context ctx = ctx();
    String state = uf.toLowerCase(Locale.ROOT);
    String url =
        dir(ctx, "ab", electionCode, state)
            + "/"
            + state
            + "-e"
            + pad(electionCode, 6)
            + "-ab.json";
    TseClient.FetchResult res = client.get(url);
    if (res.notModified()) {
      return Fetched.unchanged();
    }
    JsonNode file = parse(res.body(), url);
    validateProgressFile(file, url);
    CountingProgress progress = null;
    List<AreaProgressView> cities = new ArrayList<>();
    for (JsonNode entry : file.path("abr")) {
      String tp = text(entry, "tpabr");
      String cd = TseNumbers.asCode(scalar(entry, "cdabr"));
      if ("uf".equalsIgnoreCase(tp)) {
        progress = toProgress(entry);
      } else if ("mu".equalsIgnoreCase(tp) || "mun".equalsIgnoreCase(tp)) {
        String city = pad(cd, 5);
        cities.add(
            new AreaProgressView(
                state + "-" + city, "city", state.toUpperCase(Locale.ROOT), toProgress(entry)));
      }
    }
    if (progress == null) {
      throw new TsePayloadException("EA15 without a uf entry", url);
    }
    return Fetched.of(
        new StateProgress(state.toUpperCase(Locale.ROOT), progress, cities),
        provenance(url, file, res));
  }

  private Fetched<AreaResultView> getResult(
      TseOffice office, String areaKey, String areaType, String stateCode, String cityCode) {
    Context ctx = ctx();
    String ele = office.providerElectionCode();
    String suffix = "-c" + pad(office.code(), 4) + "-e" + pad(ele, 6) + "-u.json";
    String ufPath = stateCode == null ? "br" : stateCode.toLowerCase(Locale.ROOT);
    String fileName;
    String expectedCdabr;
    if ("country".equals(areaType)) {
      fileName = "br" + suffix;
      expectedCdabr = "br";
    } else if ("state".equals(areaType)) {
      fileName = ufPath + suffix;
      expectedCdabr = ufPath;
    } else {
      fileName = ufPath + cityCode + suffix;
      expectedCdabr = cityCode;
    }
    String url = dir(ctx, "u", ele, ufPath) + "/" + fileName;
    TseClient.FetchResult res = client.get(url);
    if (res.notModified()) {
      return Fetched.unchanged();
    }
    JsonNode file = parse(res.body(), url);
    validateResultFile(file, url);
    if (!sameCode(TseNumbers.asCode(scalar(file, "cdabr")), expectedCdabr)) {
      throw new TsePayloadException("cdabr does not match " + expectedCdabr, url);
    }
    return Fetched.of(toResult(file, office, areaKey, areaType, stateCode), provenance(url, file, res));
  }

  private AreaResultView toResult(
      JsonNode file, TseOffice office, String areaKey, String areaType, String stateCode) {
    boolean publishable = !"n".equalsIgnoreCase(text(file, "dv"));
    boolean finalResult = "s".equalsIgnoreCase(text(file, "tf"));
    JsonNode carg = null;
    if (file.path("carg").isArray()) {
      for (JsonNode c : file.get("carg")) {
        if (sameCode(TseNumbers.asCode(scalar(c, "cd")), office.code())) {
          carg = c;
          break;
        }
      }
      if (carg == null && !file.get("carg").isEmpty()) {
        carg = file.get("carg").get(0);
      }
    }

    List<CandidateResult> candidates = new ArrayList<>();
    List<PartyResult> parties = new ArrayList<>();
    if (carg != null) {
      for (JsonNode agr : carg.path("agr")) {
        String coalition =
            agr.path("tp").asText("").isEmpty() || "i".equals(text(agr, "tp"))
                ? null
                : firstNonBlank(text(agr, "com"), text(agr, "nm"));
        for (JsonNode par : agr.path("par")) {
          String partyNumber = TseNumbers.asCode(scalar(par, "n"));
          String abbr = cleanParty(text(par, "sg"));
          String partyName = text(par, "nm") == null ? "" : text(par, "nm");
          parties.add(
              new PartyResult(
                  partyNumber,
                  abbr,
                  partyName,
                  TseNumbers.toLong(scalar(par, "tvtn")),
                  TseNumbers.toLong(scalar(par, "tvtl"))));
          for (JsonNode c : par.path("cand")) {
            String number = TseNumbers.asCode(scalar(c, "n"));
            Object sq = scalar(c, "sqcand");
            String key = sq != null ? String.valueOf(sq) : office.code() + "-" + number;
            Double percent =
                publishable
                    ? firstDec(scalar(c, "pvapn"), scalar(c, "pvap"))
                    : null;
            Boolean elected =
                "s".equalsIgnoreCase(text(c, "e")) ? Boolean.TRUE : (finalResult ? Boolean.FALSE : null);
            candidates.add(
                new CandidateResult(
                    key,
                    number,
                    firstNonBlank(text(c, "nm"), text(c, "nmu"), number),
                    firstNonBlank(text(c, "nmu"), text(c, "nm"), number),
                    partyNumber,
                    abbr,
                    partyName,
                    coalition,
                    TseNumbers.toLong(scalar(c, "vap")),
                    percent,
                    elected,
                    emptyToNull(text(c, "st")),
                    emptyToNull(text(c, "dvt"))));
          }
        }
      }
    }

    JsonNode v = file.path("v");
    Long vn = TseNumbers.toLong(scalar(v, "vn"));
    Long vnt = TseNumbers.toLong(scalar(v, "vnt"));
    Long nullVotes = TseNumbers.toLong(scalar(v, "tvn"));
    if (nullVotes == null && (vn != null || vnt != null)) {
      nullVotes = (vn == null ? 0L : vn) + (vnt == null ? 0L : vnt);
    }

    CountingProgress progress = toProgress(file);
    return new AreaResultView(
        office.code(),
        areaKey,
        areaType,
        stateCode,
        progress,
        new VotesSummary(
            TseNumbers.toLong(scalar(v, "tv")),
            TseNumbers.toLong(scalar(v, "vv")),
            TseNumbers.toLong(scalar(v, "vnom")),
            TseNumbers.toLong(scalar(v, "vl")),
            TseNumbers.toLong(scalar(v, "vb")),
            nullVotes,
            TseNumbers.toLong(scalar(v, "van")),
            TseNumbers.toLong(scalar(v, "vansj"))),
        candidates,
        parties,
        carg == null ? null : TseNumbers.toInt(scalar(carg, "nv")),
        finalResult,
        publishable);
  }

  private CountingProgress toProgress(JsonNode entry) {
    JsonNode s = entry.path("s");
    JsonNode e = entry.path("e");
    String and = text(entry, "and");
    return new CountingProgress(
        STATUS.getOrDefault(and == null ? "" : and, "not-started"),
        TseNumbers.toInt(scalar(s, "ts")),
        TseNumbers.toInt(scalar(s, "st")),
        firstDec(scalar(s, "pstn"), scalar(s, "pst")),
        TseNumbers.toInt(scalar(s, "si")),
        TseNumbers.toInt(scalar(s, "sni")),
        TseNumbers.toLong(scalar(e, "te")),
        TseNumbers.toLong(scalar(e, "est")),
        TseNumbers.toLong(scalar(e, "c")),
        firstDec(scalar(e, "pcn"), scalar(e, "pc")),
        TseNumbers.toLong(scalar(e, "a")),
        firstDec(scalar(e, "pan"), scalar(e, "pa")),
        TseTime.brasiliaToUtc(text(entry, "dt"), text(entry, "ht")));
  }

  private TseOffice toOffice(JsonNode cp, JsonNode election) {
    String code = TseNumbers.asCode(scalar(cp, "cd"));
    String[] known = KNOWN_OFFICES.get(code);
    String name = text(cp, "ds") == null ? code : text(cp, "ds");
    String slug = known != null ? known[0] : slugify(name);
    String kind =
        known != null ? known[1] : ("2".equals(text(cp, "tp")) ? "proportional" : "majoritarian");
    String scope = known != null ? known[2] : "state";
    return new TseOffice(code, slug, name, kind, scope, TseNumbers.asCode(scalar(election, "cd")));
  }

  private JsonNode pickPleito(JsonNode file, String url) {
    JsonNode pl = file.path("pl");
    if (!pl.isArray() || pl.isEmpty()) {
      throw new TsePayloadException("ele-c without pl", url);
    }
    if (providerRoundId != null && !providerRoundId.isBlank()) {
      for (JsonNode p : pl) {
        if (providerRoundId.equals(TseNumbers.asCode(scalar(p, "cd")))) {
          return p;
        }
      }
    }
    String round = String.valueOf(roundNumber);
    if (roundDate != null) {
      for (JsonNode p : pl) {
        if (roundDate.equals(TseTime.isoDate(text(p, "dt")))) {
          for (JsonNode e : p.path("e")) {
            if (round.equals(TseNumbers.asCode(scalar(e, "t")))) {
              return p;
            }
          }
        }
      }
    }
    for (JsonNode p : pl) {
      for (JsonNode e : p.path("e")) {
        if (round.equals(TseNumbers.asCode(scalar(e, "t")))) {
          return p;
        }
      }
    }
    throw new TsePayloadException("no pleito for round " + round, url);
  }

  private Context ctx() {
    if (context == null) {
      getElectionConfig();
    }
    return context;
  }

  private String dir(Context ctx, String type, String electionCode, String uf) {
    String template = ctx.dirs.get(type);
    String usable =
        template != null && template.startsWith("<base>")
            ? template
            : DEFAULT_DIRS.getOrDefault(type, DEFAULT_DIRS.get("u"));
    return usable
        .replace("<base>", baseUrl)
        .replace("<ambiente>", environment)
        .replace("<ciclo>", ctx.cycle)
        .replace("<cd_eleicao>", electionCode)
        .replace("<cd_pleito>", ctx.pleito)
        .replace("<uf>", uf);
  }

  private Provenance provenance(String url, JsonNode file, TseClient.FetchResult res) {
    String sourceFile = url.startsWith(baseUrl) ? url.substring(baseUrl.length()) : url;
    return new Provenance(
        "TSE",
        ID + "@" + VERSION,
        sourceFile,
        scalar(file, "idg") == null ? null : String.valueOf(scalar(file, "idg")),
        Instant.now(),
        TseTime.brasiliaToUtc(text(file, "dg"), text(file, "hg")),
        res.etag(),
        res.checksum());
  }

  private JsonNode parse(String body, String url) {
    try {
      return mapper.readTree(body);
    } catch (Exception e) {
      throw new TsePayloadException("invalid JSON", url);
    }
  }

  private static void validateElectionConfig(JsonNode file, String url) {
    if (!file.path("pl").isArray() || file.path("pl").isEmpty()) {
      throw new TsePayloadException("payload does not match election config schema", url);
    }
  }

  private static void validateProgressFile(JsonNode file, String url) {
    if (!file.has("ele") || !file.path("abr").isArray()) {
      throw new TsePayloadException("payload does not match progress schema", url);
    }
  }

  private static void validateResultFile(JsonNode file, String url) {
    if (!file.has("ele") || !file.has("cdabr") || !file.has("tpabr")) {
      throw new TsePayloadException("payload does not match result schema", url);
    }
  }

  static String pad(String value, int width) {
    String s = value == null ? "" : value;
    if (s.length() >= width) {
      return s;
    }
    return "0".repeat(width - s.length()) + s;
  }

  static boolean sameCode(String a, String b) {
    if (a == null || b == null) {
      return false;
    }
    return normCode(a).equals(normCode(b));
  }

  private static String normCode(String x) {
    return x.matches("^\\d+$") ? String.valueOf(Long.parseLong(x)) : x.toLowerCase(Locale.ROOT);
  }

  private static String cleanParty(String sg) {
    if (sg == null) {
      return "";
    }
    return sg.replaceAll("\\*+$", "").trim();
  }

  private static String slugify(String name) {
    return name.toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", "-")
        .replaceAll("^-|-$", "");
  }

  private static Double firstDec(Object a, Object b) {
    Double x = TseNumbers.toDec(a);
    return x != null ? x : TseNumbers.toDec(b);
  }

  private static String firstNonBlank(String... values) {
    for (String v : values) {
      if (v != null && !v.isBlank()) {
        return v;
      }
    }
    return null;
  }

  private static String emptyToNull(String s) {
    return s == null || s.isBlank() ? null : s;
  }

  private static String text(JsonNode node, String field) {
    JsonNode n = node.path(field);
    if (n.isMissingNode() || n.isNull()) {
      return null;
    }
    return n.asText();
  }

  private static Object scalar(JsonNode node, String field) {
    JsonNode n = node.path(field);
    if (n.isMissingNode() || n.isNull()) {
      return null;
    }
    if (n.isNumber()) {
      return n.numberValue();
    }
    return n.asText();
  }

  private static String trimSlash(String base) {
    if (base == null) {
      return "";
    }
    return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
  }

  private record Context(
      String cycle, String pleito, Map<String, String> dirs, ElectionConfig config) {}
}
