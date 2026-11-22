package com.universal.erp.platform.ui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;


@Component
public class PosSettings {

    public record Values(boolean voice, double volume, double rate, String ttsUrl, boolean global) {
        public static Values defaults() {
            return new Values(true, 1.0, 1.0, "", true);
        }
    }

    private final ObjectMapper mapper = new ObjectMapper();
    private final Path dir = Path.of(System.getProperty("user.home"), ".uzmark", "settings");
    private final Map<String, Values> cache = new ConcurrentHashMap<>();

    public Values get(String company) {
        return cache.computeIfAbsent(key(company), this::load);
    }

    public void save(String company, Values in) {
        String ttsUrl = in.ttsUrl() == null ? "" : in.ttsUrl().trim();
        if (!(ttsUrl.startsWith("https://") || ttsUrl.startsWith("http://")) || !ttsUrl.contains("{text}")) {
            ttsUrl = "";
        }
        Values v = new Values(in.voice(), clamp(in.volume(), 0, 1), clamp(in.rate(), 0.5, 2), ttsUrl, in.global());
        String k = key(company);
        cache.put(k, v);
        try {
            Files.createDirectories(dir);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("voice", v.voice());
            m.put("volume", v.volume());
            m.put("rate", v.rate());
            m.put("ttsUrl", v.ttsUrl());
            m.put("global", v.global());
            mapper.writeValue(dir.resolve(k + ".json").toFile(), m);
        } catch (Exception ignored) {
        }
    }

    private Values load(String k) {
        try {
            Path f = dir.resolve(k + ".json");
            if (Files.exists(f)) {
                JsonNode n = mapper.readTree(f.toFile());
                return new Values(n.path("voice").asBoolean(true), clamp(n.path("volume").asDouble(1), 0, 1),
                        clamp(n.path("rate").asDouble(1), 0.5, 2), n.path("ttsUrl").asText(""),
                        n.path("global").asBoolean(true));
            }
        } catch (Exception ignored) {
        }
        return Values.defaults();
    }

    private static String key(String company) {
        String c = company == null ? "" : company;
        return c.replaceAll("[^A-Za-z0-9_-]", "_") + "-" + Integer.toHexString(c.hashCode());
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}