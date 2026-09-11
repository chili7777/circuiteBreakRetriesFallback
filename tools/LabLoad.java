import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.LockSupport;
import java.util.regex.Pattern;

/** JDK 17+, no libraries. Teaching aid, NOT a k6 implementation or capacity certification. */
public class LabLoad {
    static final String LOCAL = "http://localhost:8080";
    static final Pattern CONFIRMED = Pattern.compile("\"status\"\\s*:\\s*\"CONFIRMED\"");
    static final Pattern CONTROLLED = Pattern.compile("\"status\"\\s*:\\s*\"(REJECTED|UNAVAILABLE|PENDING)\"");
    static final Pattern UP = Pattern.compile("\"status\"\\s*:\\s*\"UP\"");
    static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2)).version(HttpClient.Version.HTTP_1_1).build();
    record Stage(String name, int rate, int seconds) {}
    static class Stats {
        final Stage stage;
        final LongAdder sent = new LongAdder(), completed = new LongAdder(), confirmed = new LongAdder();
        final LongAdder controlled = new LongAdder(), httpErrors = new LongAdder(), transportErrors = new LongAdder();
        final LongAdder dropped = new LongAdder(), invalid = new LongAdder();
        final List<Double> milliseconds = Collections.synchronizedList(new ArrayList<>());
        Stats(Stage stage) { this.stage = stage; }
        long scheduled() { return (long) stage.rate() * stage.seconds(); }
        double success() { return sent.sum() == 0 ? 0 : (double) confirmed.sum() / sent.sum(); }
        double percentile(double p) {
            List<Double> copy;
            synchronized (milliseconds) { copy = new ArrayList<>(milliseconds); }
            Collections.sort(copy);
            return copy.isEmpty() ? Double.NaN : copy.get(Math.max(0, (int)Math.ceil(p * copy.size()) - 1));
        }
    }
    public static void main(String[] args) {
        try { System.exit(run(args)); }
        catch (Exception ex) {
            System.err.println("ERROR: " + ex.getMessage());
            System.err.println("Usage: java tools/LabLoad.java check [ordersUrl] [paymentUrl]");
            System.err.println("       java tools/LabLoad.java failure <percent 0..100> <delayMs 0..10000> [paymentUrl]");
            System.err.println("       java tools/LabLoad.java <smoke|baseline|load|stress|spike|soak|outage> [ordersUrl] [seconds] [maxInFlight]");
            System.exit(2);
        }
    }
    static int run(String[] args) throws Exception {
        String scenario = args.length == 0 ? "check" : args[0];
        if (scenario.equals("check")) {
            System.out.println("Java " + System.getProperty("java.version") + "; OS " + System.getProperty("os.name"));
            for (String base : List.of(args.length > 1 ? args[1] : LOCAL,
                    args.length > 2 ? args[2] : "http://localhost:8081")) {
                HttpResponse<String> response = CLIENT.send(request(base, "/actuator/health/readiness").GET().build(), HttpResponse.BodyHandlers.ofString());
                System.out.println(base + " -> HTTP " + response.statusCode() + " " + response.body());
                if (response.statusCode() != 200 || !UP.matcher(response.body()).find()) return 2;
            }
            return 0;
        }
        if (scenario.equals("failure")) {
            if (args.length < 3) throw new IllegalArgumentException("Specify failure percent and delay.");
            int percent = bounded(args[1], 0, 100), delay = bounded(args[2], 0, 10000);
            String base = args.length > 3 ? args[3] : "http://localhost:8081";
            String json = "{\"failurePercent\":" + percent + ",\"delayMs\":" + delay + "}";
            HttpResponse<String> response = CLIENT.send(request(base, "/api/payments/lab/failure-mode")
                .PUT(HttpRequest.BodyPublishers.ofString(json)).build(), HttpResponse.BodyHandlers.ofString());
            System.out.println("LAB ONLY: " + base + " -> HTTP " + response.statusCode() + " " + response.body());
            return response.statusCode() == 200 ? 0 : 2;
        }
        String base = args.length > 1 ? args[1] : LOCAL;
        List<Stage> stages = new ArrayList<>(switch (scenario) {
            case "smoke" -> List.of(new Stage("smoke", 1, 30));
            case "baseline" -> List.of(new Stage("baseline", 20, 120));
            case "load" -> List.of(new Stage("load", 80, 180));
            case "soak" -> List.of(new Stage("soak", 20, 900));
            case "outage" -> List.of(new Stage("outage", 20, 120));
            case "stress" -> List.of(new Stage("normal", 20, 60), new Stage("high", 80, 60),
                new Stage("stress150", 150, 60), new Stage("stress200", 200, 60));
            case "spike" -> List.of(new Stage("normal", 20, 30), new Stage("spike", 150, 60),
                new Stage("recovery1", 20, 30), new Stage("recovery2", 20, 30),
                new Stage("recovery3", 20, 30), new Stage("recovery4", 20, 30));
            default -> throw new IllegalArgumentException("Unknown scenario: " + scenario);
        });
        if (args.length > 2) {
            if (stages.size() != 1) throw new IllegalArgumentException("Duration override is only for single-stage profiles.");
            Stage first = stages.get(0);
            stages.set(0, new Stage(first.name(), first.rate(), bounded(args[2], 1, 7200)));
            System.out.println("CUSTOM DURATION: this is NOT the default workshop profile.");
        }
        int concurrency = args.length > 3 ? bounded(args[3], 1, 1024) : 128;
        Semaphore slots = new Semaphore(concurrency);
        List<Stats> results = new ArrayList<>();
        String runId = UUID.randomUUID().toString();
        System.out.println("LAB ONLY -> " + base + "; scenario=" + scenario + "; maxInFlight=" + concurrency);
        System.out.println("Not k6. Drops include scheduler lag >100ms or occupied concurrency slots. Ctrl+C stops.");
        long start = System.nanoTime(), stageStart = start;
        for (Stage stage : stages) {
            Stats s = new Stats(stage); results.add(s);
            System.out.printf("Stage %s: %d req/s for %d s%n", stage.name(), stage.rate(), stage.seconds());
            for (long i = 0; i < s.scheduled(); i++) {
                long due = stageStart + i * 1_000_000_000L / stage.rate();
                long wait;
                while ((wait = due - System.nanoTime()) > 0) LockSupport.parkNanos(wait);
                if (System.nanoTime() - due > 100_000_000L || !slots.tryAcquire()) { s.dropped.increment(); continue; }
                String json = "{\"orderId\":\"" + runId + "-" + stage.name() + "-" + i + "\",\"amount\":10.25}";
                HttpRequest req = request(base, "/api/orders").POST(HttpRequest.BodyPublishers.ofString(json)).build();
                long sentAt = System.nanoTime(); s.sent.increment();
                try {
                    CLIENT.sendAsync(req, HttpResponse.BodyHandlers.ofString()).whenComplete((response, error) -> {
                        try {
                            s.milliseconds.add((System.nanoTime() - sentAt) / 1_000_000.0);
                            s.completed.increment();
                            if (error != null) { s.transportErrors.increment(); return; }
                            int status = response.statusCode();
                            if (status >= 400) s.httpErrors.increment();
                            if (status == 200 && CONFIRMED.matcher(response.body()).find()) s.confirmed.increment();
                            else if ((status == 200 || status == 503) && CONTROLLED.matcher(response.body()).find()) s.controlled.increment();
                            else s.invalid.increment();
                        } finally { slots.release(); }
                    });
                } catch (RuntimeException ex) {
                    s.transportErrors.increment(); s.completed.increment();
                    s.milliseconds.add((System.nanoTime() - sentAt) / 1_000_000.0); slots.release();
                }
            }
            stageStart += stage.seconds() * 1_000_000_000L;
        }
        long wait;
        while ((wait = stageStart - System.nanoTime()) > 0) LockSupport.parkNanos(wait);
        if (!slots.tryAcquire(concurrency, 10, TimeUnit.SECONDS)) throw new IllegalStateException("In-flight requests did not drain; run invalid.");
        double elapsed = (System.nanoTime() - start) / 1_000_000_000.0;
        Path dir = Path.of("reports"); Files.createDirectories(dir);
        Path report = dir.resolve("java-" + scenario + "-" + System.currentTimeMillis() + ".csv");
        boolean pass = true;
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(report, StandardCharsets.UTF_8))) {
            out.println("stage,target_rps,seconds,scheduled,sent,completed,confirmed,controlled,http_errors,transport_errors,invalid_response,dropped,business_success_sent,p95_ms,p99_ms");
            for (Stats s : results) {
                double p95 = s.percentile(.95), p99 = s.percentile(.99);
                String line = String.format(Locale.ROOT, "%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%.6f,%.3f,%.3f",
                    s.stage.name(), s.stage.rate(), s.stage.seconds(), s.scheduled(), s.sent.sum(), s.completed.sum(),
                    s.confirmed.sum(), s.controlled.sum(), s.httpErrors.sum(), s.transportErrors.sum(), s.invalid.sum(), s.dropped.sum(), s.success(), p95, p99);
                out.println(line);
                System.out.printf(Locale.ROOT, "%s: sent=%d/%d confirmed=%d controlled=%d errors=%d dropped=%d business=%.2f%% p95=%.2fms p99=%.2fms%n",
                    s.stage.name(), s.sent.sum(), s.scheduled(), s.confirmed.sum(), s.controlled.sum(),
                    s.httpErrors.sum() + s.transportErrors.sum(), s.dropped.sum(), s.success() * 100, p95, p99);
                boolean valid = s.sent.sum() > 0 && s.dropped.sum() == 0 && s.completed.sum() == s.sent.sum();
                boolean healthy = s.success() >= .99 && p95 < 500 && p99 < 1000;
                boolean outage = s.confirmed.sum() == 0 && s.controlled.sum() == s.sent.sum() && p95 < 1000;
                if (scenario.equals("stress") || scenario.equals("spike")) pass &= valid;
                else pass &= valid && (scenario.equals("outage") ? outage : healthy);
            }
        }
        long sent = results.stream().mapToLong(s -> s.sent.sum()).sum();
        System.out.printf(Locale.ROOT, "Observed send rate including final drain: %.2f req/s; elapsed %.2fs%n", sent / elapsed, elapsed);
        System.out.println("CSV: " + report.toAbsolutePath());
        System.out.println("Business success denominator = sent (including transport failures). Drops are reported separately.");
        System.out.println((pass ? "PASS" : "FAIL") + (scenario.equals("stress") || scenario.equals("spike")
            ? " generator only. Inspect phase results; not a resilience/SLO approval." : " workshop gate only; not a production or monthly SLO certificate."));
        return pass ? 0 : 1;
    }
    static HttpRequest.Builder request(String base, String path) {
        URI uri = URI.create(base.replaceAll("/+$", "") + path);
        if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) || uri.getHost() == null)
            throw new IllegalArgumentException("Use an http(s) URL for an authorized lab only.");
        return HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(3)).header("Content-Type", "application/json");
    }
    static int bounded(String value, int min, int max) {
        int n = Integer.parseInt(value);
        if (n < min || n > max) throw new IllegalArgumentException("Value must be between " + min + " and " + max);
        return n;
    }
}
