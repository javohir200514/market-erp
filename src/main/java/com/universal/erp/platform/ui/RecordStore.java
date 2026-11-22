package com.universal.erp.platform.ui;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Bo'limlardagi yozuvlar (hisob-faktura, mijoz, mahsulot, xodim ...). Barcha bo'limlar bitta
 * umumiy jadvalda (erp_records) saqlanadi; yozuv maydonlari JSON matn ko'rinishida.
 * Jadval birinchi murojaatda o'zi yaratiladi. Baza bo'lmasa, xotirada saqlanadi.
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/RecordStore.java
 */
@Service
public class RecordStore {


    public record Rec(String id, Map<String, String> data) {
        public String get(String key) {
            String v = data.get(key);
            return v == null ? "" : v;
        }
    }

    private static final Logger log = LoggerFactory.getLogger(RecordStore.class);
    private static final TypeReference<LinkedHashMap<String, String>> MAP = new TypeReference<>() { };

    private final ObjectProvider<JdbcTemplate> jdbcProvider;
    private final ObjectMapper json = new ObjectMapper();
    private final Map<String, List<Rec>> memory = new ConcurrentHashMap<>();
    private volatile boolean tableReady;

    public RecordStore(ObjectProvider<JdbcTemplate> jdbcProvider) {
        this.jdbcProvider = jdbcProvider;
    }

    /** Eng yangisi birinchi. */
    public List<Rec> list(String company, String section) {
        String c = norm(company);
        JdbcTemplate jdbc = jdbc();
        if (jdbc != null) {
            try {
                return jdbc.query(
                        "SELECT id, data FROM erp_records WHERE company_login = ? AND section_key = ? "
                                + "ORDER BY created_at DESC, id",
                        (rs, i) -> new Rec(rs.getString("id"), read(rs.getString("data"))), c, section);
            } catch (RuntimeException ex) {
                log.warn("Yozuvlarni o'qib bo'lmadi ({} / {}): {}", c, section, ex.toString());
            }
        }
        List<Rec> list = memory.get(c + "|" + section);
        return list == null ? List.of() : new ArrayList<>(list);
    }

    /** id == null bo'lsa yangi yozuv, aks holda mavjudini yangilaydi. @throws IllegalStateException saqlab bo'lmasa */
    public Rec save(String company, String section, String id, Map<String, String> data) {
        String c = norm(company);
        Map<String, String> clean = new LinkedHashMap<>();
        data.forEach((k, v) -> {
            if (v != null && !v.isBlank()) {
                clean.put(k, v.trim());
            }
        });
        String rid = id == null ? UUID.randomUUID().toString() : id;

        JdbcTemplate jdbc = jdbc();
        if (jdbc != null) {
            try {
                String text = json.writeValueAsString(clean);
                if (id == null) {
                    jdbc.update("INSERT INTO erp_records (id, company_login, section_key, data) VALUES (?, ?, ?, ?)",
                            rid, c, section, text);
                } else {
                    jdbc.update("UPDATE erp_records SET data = ? WHERE id = ? AND company_login = ?", text, rid, c);
                }
                return new Rec(rid, clean);
            } catch (Exception ex) {
                log.warn("Yozuvni saqlab bo'lmadi ({} / {}): {}", c, section, ex.toString());
                throw new IllegalStateException(ex);
            }
        }

        List<Rec> list = memory.computeIfAbsent(c + "|" + section, k -> Collections.synchronizedList(new ArrayList<>()));
        Rec rec = new Rec(rid, clean);
        synchronized (list) {
            if (id == null) {
                list.add(0, rec);
            } else {
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).id().equals(id)) {
                        list.set(i, rec);
                    }
                }
            }
        }
        return rec;
    }

    public void delete(String company, String section, String id) {
        String c = norm(company);
        JdbcTemplate jdbc = jdbc();
        if (jdbc != null) {
            try {
                jdbc.update("DELETE FROM erp_records WHERE id = ? AND company_login = ?", id, c);
                return;
            } catch (RuntimeException ex) {
                log.warn("Yozuvni o'chirib bo'lmadi ({} / {}): {}", c, section, ex.toString());
                throw new IllegalStateException(ex);
            }
        }
        List<Rec> list = memory.get(c + "|" + section);
        if (list != null) {
            list.removeIf(r -> r.id().equals(id));
        }
    }
    /** ID bo'yicha bitta yozuv. */
    public Optional<Rec> find(String company, String section, String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        String c = norm(company);
        JdbcTemplate jdbc = jdbc();
        if (jdbc != null) {
            try {
                return jdbc.query(
                        "SELECT id, data FROM erp_records WHERE id = ? AND company_login = ? AND section_key = ?",
                        (rs, i) -> new Rec(rs.getString("id"), read(rs.getString("data"))),
                        id, c, section).stream().findFirst();
            } catch (RuntimeException ex) {
                log.warn("Yozuvni o'qib bo'lmadi ({} / {} / {}): {}", c, section, id, ex.toString());
            }
        }
        List<Rec> list = memory.get(c + "|" + section);
        if (list == null) {
            return Optional.empty();
        }
        synchronized (list) {
            return list.stream().filter(r -> r.id().equals(id)).findFirst();
        }
    }

    // ------------------------------------------------------------------ ichki

    private Map<String, String> read(String text) {
        try {
            return json.readValue(text, MAP);
        } catch (Exception ex) {
            return new LinkedHashMap<>();
        }
    }

    private JdbcTemplate jdbc() {
        JdbcTemplate jdbc = jdbcProvider.getIfAvailable();
        if (jdbc == null) {
            return null;
        }
        if (!tableReady) {
            try {
                jdbc.execute("CREATE TABLE IF NOT EXISTS erp_records ("
                        + "id VARCHAR(36) PRIMARY KEY, "
                        + "company_login VARCHAR(100) NOT NULL, "
                        + "section_key VARCHAR(60) NOT NULL, "
                        + "data TEXT NOT NULL, "
                        + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
                try {
                    jdbc.execute("CREATE INDEX idx_erp_records_scope ON erp_records (company_login, section_key)");
                } catch (RuntimeException ignored) {
                    // indeks allaqachon bor
                }
                tableReady = true;
            } catch (RuntimeException ex) {
                log.warn("erp_records jadvalini yaratib bo'lmadi: {}", ex.toString());
                return null;
            }
        }
        return jdbc;
    }

    private static String norm(String s) {
        return CurrentCompany.key(s);
    }
}