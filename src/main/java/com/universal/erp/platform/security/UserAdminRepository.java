package com.universal.erp.platform.security;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Kompaniya xodimlarini boshqarish uchun SQL (users, roles, user_roles). Faqat SQL — qoidalar {@link UserAdminService}'da.
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/security/UserAdminRepository.java
 */
@Repository
public class UserAdminRepository {

    public record MemberRow(UUID id, String email, String firstName, String lastName,
                            List<String> roles, boolean blocked, OffsetDateTime lastLoginAt) {
    }

    private final JdbcClient jdbc;

    public UserAdminRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UUID> tenantIdBySlug(String slug) {
        return jdbc.sql("SELECT id FROM tenants WHERE slug = :slug")
                .param("slug", slug)
                .query(UUID.class)
                .optional();
    }

    public List<MemberRow> listMembers(UUID tenantId) {
        return jdbc.sql("""
                        SELECT u.id, u.email, u.first_name, u.last_name, u.last_login_at,
                               (u.locked_until IS NOT NULL AND u.locked_until > now()) AS blocked,
                               COALESCE((SELECT string_agg(r.code, ',' ORDER BY r.code)
                                         FROM user_roles ur
                                         JOIN roles r ON r.id = ur.role_id AND r.tenant_id = ur.tenant_id
                                         WHERE ur.user_id = u.id AND ur.tenant_id = u.tenant_id), '') AS roles
                        FROM users u
                        WHERE u.tenant_id = :tenantId
                        ORDER BY u.created_at
                        """)
                .param("tenantId", tenantId)
                .query((rs, i) -> {
                    String roles = rs.getString("roles");
                    return new MemberRow(
                            rs.getObject("id", UUID.class),
                            rs.getString("email"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            roles == null || roles.isBlank() ? List.of() : Arrays.asList(roles.split(",")),
                            rs.getBoolean("blocked"),
                            rs.getObject("last_login_at", OffsetDateTime.class));
                })
                .list();
    }

    public boolean emailExists(UUID tenantId, String email) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM users WHERE tenant_id = :t AND lower(email) = :e)")
                .param("t", tenantId)
                .param("e", email)
                .query(Boolean.class)
                .single();
    }

    public Optional<UUID> findRoleId(UUID tenantId, String code) {
        return jdbc.sql("SELECT id FROM roles WHERE tenant_id = :t AND code = :c")
                .param("t", tenantId)
                .param("c", code)
                .query(UUID.class)
                .optional();
    }

    public Optional<String> findEmail(UUID tenantId, UUID userId) {
        return jdbc.sql("SELECT email FROM users WHERE tenant_id = :t AND id = :u")
                .param("t", tenantId)
                .param("u", userId)
                .query(String.class)
                .optional();
    }

    public boolean userInTenant(UUID tenantId, UUID userId) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM users WHERE tenant_id = :t AND id = :u)")
                .param("t", tenantId)
                .param("u", userId)
                .query(Boolean.class)
                .single();
    }

    /** Foydalanuvchining barcha rollarini olib tashlaydi (keyin yangisi beriladi). */
    public void clearRoles(UUID tenantId, UUID userId) {
        jdbc.sql("DELETE FROM user_roles WHERE tenant_id = :t AND user_id = :u")
                .param("t", tenantId)
                .param("u", userId)
                .update();
    }

    public void setPassword(UUID tenantId, UUID userId, String hash) {
        jdbc.sql("""
                        UPDATE users
                        SET password_hash = :h, failed_login_attempts = 0,
                            updated_at = now(), version = version + 1
                        WHERE id = :u AND tenant_id = :t
                        """)
                .param("h", hash)
                .param("u", userId)
                .param("t", tenantId)
                .update();
    }

    /** Bloklash: login'dagi mavjud "locked_until" mexanizmidan foydalanamiz (status ustuniga tegmaymiz). */
    public void setBlocked(UUID tenantId, UUID userId, boolean blocked) {
        jdbc.sql("""
                        UPDATE users
                        SET locked_until = CASE WHEN :blocked THEN now() + interval '100 years' ELSE NULL END,
                            failed_login_attempts = 0,
                            updated_at = now(), version = version + 1
                        WHERE id = :u AND tenant_id = :t
                        """)
                .param("blocked", blocked)
                .param("u", userId)
                .param("t", tenantId)
                .update();
    }
}