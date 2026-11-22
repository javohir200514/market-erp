package com.universal.erp.platform.ui;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kompaniya tanlagan yo'nalishlar. Kalit: kompaniya kirish nomi (TenantModules kabi).
 * Joylashuvi: src/main/java/com/universal/erp/retail/business/CompanyBusinessTypes.java
 */
@Service
public class CompanyBusinessTypes {

    private final JdbcTemplate jdbc;

    public CompanyBusinessTypes(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void save(String company, Set<BusinessType> types) {
        if (company == null || company.isBlank() || types == null || types.isEmpty()) {
            throw new IllegalArgumentException("Kompaniya va kamida bitta yo'nalish kerak");
        }
        String key = company.trim().toLowerCase(Locale.ROOT);
        jdbc.update("DELETE FROM company_business_types WHERE company_login = ?", key);
        for (BusinessType t : types) {
            jdbc.update("INSERT INTO company_business_types (company_login, business_type) VALUES (?, ?)",
                    key, t.name());
        }
    }

    @Transactional(readOnly = true)
    public Set<BusinessType> find(String company) {
        Set<BusinessType> result = EnumSet.noneOf(BusinessType.class);
        if (company == null || company.isBlank()) {
            return result;
        }
        List<String> rows = jdbc.queryForList(
                "SELECT business_type FROM company_business_types WHERE company_login = ?",
                String.class, company.trim().toLowerCase(Locale.ROOT));
        for (String r : rows) {
            try {
                result.add(BusinessType.valueOf(r));
            } catch (IllegalArgumentException ignored) {
                // eskirgan qiymat e'tiborsiz qoldiriladi
            }
        }
        return result;
    }
}