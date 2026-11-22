package com.universal.erp.platform.ui;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Kompaniya tanlagan modullar. Jadval (tenant_modules) birinchi murojaatda o'zi yaratiladi.
 * Bazaga ulanib bo'lmasa, xotirada saqlanadi (server qayta ishga tushguncha).
 * Eski kompaniyalar (yozuvi yo'q) uchun barcha modullar ochiq qoladi.
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/TenantModules.java
 */
@Service
public class TenantModules {

    private static final Logger log = LoggerFactory.getLogger(TenantModules.class);

    private final ObjectProvider<JdbcTemplate> jdbcProvider;
    private final Map<String, Set<AppModule>> memory = new ConcurrentHashMap<>();
    private volatile boolean tableReady;

    public TenantModules(ObjectProvider<JdbcTemplate> jdbcProvider) {
        this.jdbcProvider = jdbcProvider;
    }

    public void save(String company, Set<AppModule> modules) {
        String c = norm(company);
        if (c.isEmpty() || modules == null || modules.isEmpty()) {
            return;
        }
        memory.put(c, EnumSet.copyOf(modules));
        JdbcTemplate jdbc = jdbc();
        if (jdbc == null) {
            return;
        }
        try {
            jdbc.update("DELETE FROM tenant_modules WHERE company_login = ?", c);
            for (AppModule m : modules) {
                jdbc.update("INSERT INTO tenant_modules (company_login, module_key) VALUES (?, ?)", c, m.key());
            }
        } catch (RuntimeException ex) {
            log.warn("Modullarni bazaga yozib bo'lmadi (kompaniya={}): {}", c, ex.toString());
        }
    }

    /** Kompaniyaga ruxsat etilgan modullar; yozuv bo'lmasa — hammasi. */
    public Set<AppModule> enabled(String company) {
        String c = norm(company);
        if (c.isEmpty()) {
            return EnumSet.allOf(AppModule.class);
        }
        Set<AppModule> cached = memory.get(c);
        if (cached != null) {
            return EnumSet.copyOf(cached);
        }
        JdbcTemplate jdbc = jdbc();
        if (jdbc != null) {
            try {
                Set<AppModule> set = EnumSet.noneOf(AppModule.class);
                for (String key : jdbc.queryForList(
                        "SELECT module_key FROM tenant_modules WHERE company_login = ?", String.class, c)) {
                    AppModule.byKey(key).ifPresent(set::add);
                }
                if (!set.isEmpty()) {
                    memory.put(c, set);
                    return EnumSet.copyOf(set);
                }
            } catch (RuntimeException ex) {
                log.warn("Modullarni o'qib bo'lmadi (kompaniya={}): {}", c, ex.toString());
            }
        }
        return EnumSet.allOf(AppModule.class);
    }

    private JdbcTemplate jdbc() {
        JdbcTemplate jdbc = jdbcProvider.getIfAvailable();
        if (jdbc == null) {
            return null;
        }
        if (!tableReady) {
            try {
                jdbc.execute("CREATE TABLE IF NOT EXISTS tenant_modules ("
                        + "company_login VARCHAR(100) NOT NULL, "
                        + "module_key VARCHAR(30) NOT NULL, "
                        + "PRIMARY KEY (company_login, module_key))");
                tableReady = true;
            } catch (RuntimeException ex) {
                log.warn("tenant_modules jadvalini yaratib bo'lmadi: {}", ex.toString());
                return null;
            }
        }
        return jdbc;
    }

    private static String norm(String s) {
        return CurrentCompany.key(s);
    }
}