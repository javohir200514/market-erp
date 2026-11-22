
package com.universal.erp.platform.ui;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
class LoginCompanyResolverImpl implements LoginCompanyResolver {

    private static final String SQL = """
            SELECT DISTINCT t.slug
            FROM users u
            JOIN tenants t ON t.id = u.tenant_id
            WHERE lower(u.email) = ?
               OR lower(split_part(u.email, '@', 1)) = ?
            """;

    private final JdbcTemplate jdbc;

    LoginCompanyResolverImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<String> resolveCompany(String identifier) {

        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }

        String id = identifier.trim().toLowerCase(Locale.ROOT);

        List<String> found = jdbc.queryForList(
                SQL,
                String.class,
                id,
                id
        );

        // Faqat bitta tenant topilsa ruxsat beriladi.
        // 0 ta yoki bir nechta natija bo'lsa login rad etiladi.
        if (found.size() != 1) {
            return Optional.empty();
        }

        return Optional.of(found.get(0));
    }
}
