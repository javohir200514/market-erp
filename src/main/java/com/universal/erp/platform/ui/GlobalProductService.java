package com.universal.erp.platform.ui;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;


@Component
public class GlobalProductService {

    public record Global(String barcode, String name, String brand, String category,
                         String quantity, String image, String imageFront) {
        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("barcode", barcode);
            m.put("name", name);
            m.put("brand", brand);
            m.put("category", category);
            m.put("quantity", quantity);
            m.put("image", image);
            m.put("imageFront", imageFront);
            return m;
        }
    }

    private record Entry(long ts, Global g) {
    }

    private static final long HIT_MS = Duration.ofDays(30).toMillis();
    private static final long MISS_MS = Duration.ofDays(1).toMillis();
    private static final String URL = "https://world.openfoodfacts.org/api/v3/product/%s.json"
            + "?fields=code,product_name,brands,categories,quantity,image_url,image_front_url";
    private static final String TEST_BARCODE = "3017620422003";

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Path dir = Path.of(System.getProperty("user.home"), ".uzmark", "global");
    private final Map<String, Entry> mem = new ConcurrentHashMap<>();

    private volatile Boolean lastOk;
    private volatile long lastCheck;
    private volatile String lastError = "";

    // ------------------------------------------------------------ holat

    public Boolean lastOk() { return lastOk; }
    public long lastCheck() { return lastCheck; }
    public String lastError() { return lastError; }

    /** Ulanishni tekshiradi (sozlamalar sahifasi uchun). */
    public boolean check() {
        fetch(TEST_BARCODE);
        return Boolean.TRUE.equals(lastOk);
    }

    // ------------------------------------------------------------ qidirish

    public Optional<Global> lookup(String barcode) {
        if (barcode == null || !barcode.matches("\\d{6,14}")) {
            return Optional.empty();
        }
        Entry e = mem.get(barcode);
        if (e == null) {
            e = readFile(barcode);
            if (e != null) {
                mem.put(barcode, e);
            }
        }
        if (e != null) {
            long age = System.currentTimeMillis() - e.ts();
            if (age < (e.g() == null ? MISS_MS : HIT_MS)) {
                return Optional.ofNullable(e.g());
            }
        }
        Entry fresh = fetch(barcode);
        if (fresh != null) {
            mem.put(barcode, fresh);
            writeFile(barcode, fresh);
            return Optional.ofNullable(fresh.g());
        }
        // API ishlamadi: eskirgan keshdan bo'lsa ham foydalanamiz
        return e == null ? Optional.empty() : Optional.ofNullable(e.g());
    }

    /** null = API xatosi (keshlanmaydi); Entry(g=null) = mahsulot yo'q. */
    private Entry fetch(String barcode) {
        lastCheck = System.currentTimeMillis();
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(String.format(URL, barcode)))
                    .timeout(Duration.ofSeconds(3))
                    .header("User-Agent", "UzMarkERP/1.0")
                    .header("Accept", "application/json")
                    .GET().build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            int code = res.statusCode();
            if (code != 200 && code != 404) {
                lastOk = false;
                lastError = "HTTP " + code;
                return null;
            }
            lastOk = true;
            lastError = "";
            JsonNode p = mapper.readTree(res.body()).path("product");
            String name = text(p, "product_name");
            if (code == 404 || p.isMissingNode() || name.isEmpty()) {
                return new Entry(System.currentTimeMillis(), null);
            }
            String img = https(text(p, "image_url"));
            String front = https(text(p, "image_front_url"));
            Global g = new Global(barcode, name, text(p, "brands"), text(p, "categories"),
                    text(p, "quantity"), img, front.isEmpty() ? img : front);
            return new Entry(System.currentTimeMillis(), g);
        } catch (Exception ex) {
            lastOk = false;
            lastError = ex.getClass().getSimpleName();
            return null;
        }
    }

    // ------------------------------------------------------------ fayl keshi

    private Entry readFile(String barcode) {
        try {
            Path f = dir.resolve(barcode + ".json");
            if (!Files.exists(f)) {
                return null;
            }
            JsonNode n = mapper.readTree(f.toFile());
            long ts = n.path("ts").asLong(0);
            if (!n.path("found").asBoolean(false)) {
                return new Entry(ts, null);
            }
            return new Entry(ts, new Global(barcode, text(n, "name"), text(n, "brand"), text(n, "category"),
                    text(n, "quantity"), text(n, "image"), text(n, "imageFront")));
        } catch (Exception ex) {
            return null;
        }
    }

    private void writeFile(String barcode, Entry e) {
        try {
            Files.createDirectories(dir);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ts", e.ts());
            m.put("found", e.g() != null);
            if (e.g() != null) {
                m.putAll(e.g().toMap());
            }
            mapper.writeValue(dir.resolve(barcode + ".json").toFile(), m);
        } catch (Exception ignored) {
        }
    }

    private static String text(JsonNode n, String f) {
        return n.path(f).asText("").trim();
    }

    private static String https(String u) {
        return u.startsWith("https://") ? u : "";
    }
}