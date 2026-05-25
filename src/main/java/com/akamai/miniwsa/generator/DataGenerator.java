package com.akamai.miniwsa.generator;

import com.akamai.miniwsa.domain.Action;
import com.akamai.miniwsa.domain.RuleCategory;
import com.akamai.miniwsa.domain.Severity;
import com.akamai.miniwsa.ingestion.dto.EventRequest;
import com.akamai.miniwsa.ingestion.dto.GeoLocationRequest;
import com.akamai.miniwsa.ingestion.dto.RuleRequest;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Standalone data generator — no Spring context required.
 *
 * Usage:
 *   mvn exec:java -Dexec.args="--count=1000 --batch-size=100 --waves=3 --wave-size=10"
 *   mvn exec:java -Dexec.args="--count=500 --ingest=http://localhost:8080"
 *
 * Outputs numbered JSON batch files (events_000.json, events_001.json, …) into
 * the output directory. Repeated runs append new files — existing files are never
 * overwritten. Pass --ingest=<baseUrl> to also POST each newly written file to
 * the ingestion endpoint.
 */
public class DataGenerator {

    private static final List<Long> CONFIG_IDS = List.of(14227L, 8801L, 33104L, 5512L, 99001L);

    private static final List<String> HOSTNAMES = List.of(
            "www.example.com", "api.example.com", "shop.example.com", "login.example.com"
    );

    private static final List<String> PATHS = List.of(
            "/api/v1/login", "/admin/dashboard", "/api/v1/users", "/search",
            "/checkout", "/api/data", "/api/v1/products", "/account/settings"
    );

    private static final List<String> USER_AGENTS = List.of(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64; rv:124.0) Gecko/20100101 Firefox/124.0",
            "sqlmap/1.7.8#stable (https://sqlmap.org)",
            "Mozilla/5.00 (Nikto/2.1.6) (Evasions:None) (Test:Port Check)",
            "python-requests/2.31.0"
    );

    private static final List<String[]> GEO_PAIRS = List.of(
            new String[]{"CN", "Beijing"},
            new String[]{"RU", "Moscow"},
            new String[]{"US", "New York"},
            new String[]{"DE", "Berlin"},
            new String[]{"BR", "Sao Paulo"},
            new String[]{"NG", "Lagos"},
            new String[]{"KP", "Pyongyang"},
            new String[]{"IR", "Tehran"},
            new String[]{"UA", "Kyiv"},
            new String[]{"GB", "London"}
    );

    // {ruleId, name, message, category} — one entry per RuleCategory
    private static final List<Object[]> RULES = List.of(
            new Object[]{"950001", "SQL_INJECTION",       "SQL Injection Attack Detected",        RuleCategory.INJECTION},
            new Object[]{"950002", "XSS_ATTACK",          "Cross-Site Scripting Attack Detected", RuleCategory.XSS},
            new Object[]{"960901", "PROTOCOL_VIOLATION",  "Malformed HTTP Request Detected",      RuleCategory.PROTOCOL_VIOLATION},
            new Object[]{"970001", "DATA_LEAKAGE",        "Sensitive Data Exposure Detected",     RuleCategory.DATA_LEAKAGE},
            new Object[]{"990001", "BOT_ACTIVITY",        "Automated Bot Traffic Detected",       RuleCategory.BOT},
            new Object[]{"980001", "DOS_ATTACK",          "Denial of Service Pattern Detected",   RuleCategory.DOS},
            new Object[]{"990002", "RATE_LIMIT_EXCEEDED", "Rate Limit Exceeded",                  RuleCategory.RATE_LIMIT}
    );

    // Weighted pool — 403 most common (WAF block), then 400, then others
    private static final int[] STATUS_POOL = {403, 403, 403, 400, 400, 200, 301, 500};

    // Weighted method pool — GET 50%, POST 30%, PUT 10%, DELETE 10%
    private static final String[] METHOD_POOL = {
            "GET", "GET", "GET", "GET", "GET",
            "POST", "POST", "POST",
            "PUT",
            "DELETE"
    };

    private static final Instant EPOCH_2020 = Instant.parse("2020-01-01T00:00:00Z");

    // --- Entry point ---

    public static void main(String[] args) throws Exception {
        int count      = 1000;
        String outDir  = "generated-events";
        int batchSize  = 100;
        int waves      = 3;
        int waveSize   = 10;
        String ingestUrl = null;

        for (String arg : args) {
            if      (arg.startsWith("--count="))       count     = Integer.parseInt(arg.substring(8));
            else if (arg.startsWith("--output-dir="))  outDir    = arg.substring(13);
            else if (arg.startsWith("--batch-size="))  batchSize = Integer.parseInt(arg.substring(13));
            else if (arg.startsWith("--waves="))       waves     = Integer.parseInt(arg.substring(8));
            else if (arg.startsWith("--wave-size="))   waveSize  = Integer.parseInt(arg.substring(12));
            else if (arg.startsWith("--ingest="))      ingestUrl = arg.substring(9);
        }

        List<EventRequest> events = generate(count, waves, waveSize);
        printStats(events);

        ObjectMapper mapper = JsonMapper.builder()
                .configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .build();

        List<File> written = writeBatches(events, outDir, batchSize, mapper);

        System.out.printf("Generated %d events → %s/ (%d file%s of up to %d each)%n",
                events.size(), outDir, written.size(), written.size() == 1 ? "" : "s", batchSize);

        if (ingestUrl != null) {
            System.out.printf("Ingesting %d file%s to %s …%n",
                    written.size(), written.size() == 1 ? "" : "s", ingestUrl);
            ingestFiles(written, ingestUrl);
        }
    }

    // --- Core generation (no I/O — fully testable) ---

    public static List<EventRequest> generate(int count, int waves, int waveSize) {
        if (count <= 0) return List.of();

        Random rng = new Random();
        List<EventRequest> events = new ArrayList<>(count);

        Instant now = Instant.now();
        long rangeSeconds = now.getEpochSecond() - EPOCH_2020.getEpochSecond();

        // Attack waves: each wave shares a fixed IP + path + configId
        for (int w = 0; w < waves && events.size() < count; w++) {
            String waveIp     = randomIp(rng);
            String wavePath   = pick(PATHS, rng);
            long   waveConfig = pick(CONFIG_IDS, rng);
            for (int i = 0; i < waveSize && events.size() < count; i++) {
                events.add(buildEvent(waveIp, wavePath, waveConfig, rng, rangeSeconds));
            }
        }

        // Fill remaining slots with random events
        while (events.size() < count) {
            events.add(buildEvent(randomIp(rng), pick(PATHS, rng), pick(CONFIG_IDS, rng), rng, rangeSeconds));
        }

        // Shuffle so wave events are distributed throughout the list
        Collections.shuffle(events, rng);

        // Assign zero-padded sequential IDs in final list order
        for (int i = 0; i < events.size(); i++) {
            events.get(i).setEventId(String.format("evt-%05d", i + 1));
        }
        return events;
    }

    // --- Batch file writer ---

    static List<File> writeBatches(List<EventRequest> events, String outputDir,
                                   int batchSize, ObjectMapper mapper) throws Exception {
        File dir = new File(outputDir);
        dir.mkdirs();
        int fileIndex = nextFileIndex(dir);
        List<File> written = new ArrayList<>();
        for (int start = 0; start < events.size(); start += batchSize) {
            List<EventRequest> batch = events.subList(start, Math.min(start + batchSize, events.size()));
            File out = new File(dir, String.format("events_%03d.json", fileIndex++));
            mapper.writeValue(out, batch);
            written.add(out);
        }
        return written;
    }

    // --- Ingest written files ---

    static void ingestFiles(List<File> files, String baseUrl) throws Exception {
        HttpClient http = HttpClient.newHttpClient();
        String url = baseUrl + "/v1/events/ingest";
        for (File file : files) {
            String body = Files.readString(file.toPath());
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            System.out.printf("  [%s] %s → %d%n",
                    res.statusCode() == 201 ? "OK  " : "FAIL", file.getName(), res.statusCode());
        }
    }

    // --- File index helper ---

    static int nextFileIndex(File dir) {
        File[] existing = dir.listFiles((d, name) -> name.matches("events_\\d{3}\\.json"));
        if (existing == null || existing.length == 0) return 0;
        return Arrays.stream(existing)
                .mapToInt(f -> Integer.parseInt(f.getName().substring(7, 10)))
                .max().orElse(-1) + 1;
    }

    // --- Private builders ---

    private static EventRequest buildEvent(String ip, String path, long configId,
                                           Random rng, long rangeSeconds) {
        EventRequest req = new EventRequest();
        // eventId assigned post-shuffle in generate() — left null here intentionally
        req.setTimestamp(EPOCH_2020.plusSeconds(ThreadLocalRandom.current().nextLong(rangeSeconds)));
        req.setConfigId(configId);
        req.setPolicyId("pol_" + String.format("%04d", configId % 10000));
        req.setClientIp(ip);
        req.setHostname(pick(HOSTNAMES, rng));
        req.setPath(path);
        req.setMethod(METHOD_POOL[rng.nextInt(METHOD_POOL.length)]);
        req.setStatusCode(STATUS_POOL[rng.nextInt(STATUS_POOL.length)]);
        req.setUserAgent(pick(USER_AGENTS, rng));
        req.setRule(randomRule(rng));
        req.setAction(Action.values()[rng.nextInt(Action.values().length)]);
        req.setGeoLocation(randomGeo(rng));
        req.setRequestSize(rng.nextInt(8192));
        req.setResponseSize(rng.nextInt(4096));
        return req;
    }

    private static String randomIp(Random rng) {
        return (rng.nextInt(223) + 1) + "." + rng.nextInt(256) + "."
             + rng.nextInt(256) + "." + (rng.nextInt(254) + 1);
    }

    private static RuleRequest randomRule(Random rng) {
        Object[] rule = RULES.get(rng.nextInt(RULES.size()));
        RuleRequest r = new RuleRequest();
        r.setId((String) rule[0]);
        r.setName((String) rule[1]);
        r.setMessage((String) rule[2]);
        r.setCategory((RuleCategory) rule[3]);
        r.setSeverity(Severity.values()[rng.nextInt(Severity.values().length)]);
        return r;
    }

    private static GeoLocationRequest randomGeo(Random rng) {
        String[] pair = GEO_PAIRS.get(rng.nextInt(GEO_PAIRS.size()));
        GeoLocationRequest geo = new GeoLocationRequest();
        geo.setCountry(pair[0]);
        geo.setCity(pair[1]);
        return geo;
    }

    private static <T> T pick(List<T> list, Random rng) {
        return list.get(rng.nextInt(list.size()));
    }

    // --- Console statistics ---

    public static void printStats(List<EventRequest> events) {
        if (events.isEmpty()) {
            System.out.println("No events generated.");
            return;
        }

        Map<Long, ConfigStats> stats = new LinkedHashMap<>();
        for (EventRequest e : events) {
            stats.computeIfAbsent(e.getConfigId(), k -> new ConfigStats()).record(e);
        }

        System.out.println();
        System.out.println("=== Data Generator Statistics ===");
        System.out.printf("Total events generated: %d%n%n", events.size());
        System.out.printf("%-10s | %5s | %-20s | %-20s | %-34s | Top Action%n",
                "ConfigId", "Count", "Min Timestamp", "Max Timestamp", "Severity Distribution");
        System.out.println("-".repeat(115));

        stats.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    long cid = entry.getKey();
                    ConfigStats s = entry.getValue();
                    String dist = String.format("CRIT:%-4d HIGH:%-4d MED:%-4d LOW:%-4d",
                            s.bySeverity.getOrDefault(Severity.CRITICAL, 0),
                            s.bySeverity.getOrDefault(Severity.HIGH, 0),
                            s.bySeverity.getOrDefault(Severity.MEDIUM, 0),
                            s.bySeverity.getOrDefault(Severity.LOW, 0));
                    Action topAction = s.byAction.entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey).orElse(null);
                    System.out.printf("%-10d | %5d | %-20s | %-20s | %-34s | %s(%d)%n",
                            cid, s.count,
                            s.minTimestamp.toString().substring(0, 19) + "Z",
                            s.maxTimestamp.toString().substring(0, 19) + "Z",
                            dist,
                            topAction,
                            topAction == null ? 0 : s.byAction.get(topAction));
                });
        System.out.println();
    }

    private static class ConfigStats {
        int count;
        Instant minTimestamp;
        Instant maxTimestamp;
        final Map<Severity, Integer> bySeverity = new EnumMap<>(Severity.class);
        final Map<Action, Integer>   byAction    = new EnumMap<>(Action.class);

        void record(EventRequest e) {
            count++;
            Instant ts = e.getTimestamp();
            if (minTimestamp == null || ts.isBefore(minTimestamp)) minTimestamp = ts;
            if (maxTimestamp == null || ts.isAfter(maxTimestamp))  maxTimestamp = ts;
            bySeverity.merge(e.getRule().getSeverity(), 1, Integer::sum);
            byAction.merge(e.getAction(), 1, Integer::sum);
        }
    }
}
