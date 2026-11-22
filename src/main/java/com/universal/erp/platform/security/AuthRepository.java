
package com.universal.erp.platform.security;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Login va ro'yxatdan o'tish uchun ma'lumotlar qatlami.
 *
 * Barcha SQL so'rovlari Spring JdbcClient va named parameters
 * (:id, :tenantId ...) orqali bajariladi.
 */
@Repository
public class AuthRepository {

    static final String SYSTEM_ACTOR = "registration";

    public record UserRow(
            UUID id,
            UUID tenantId,
            String tenantSlug,
            String tenantStatus,
            String email,
            String firstName,
            String lastName,
            String passwordHash,
            String status,
            int failedAttempts,
            OffsetDateTime lockedUntil,
            String locale,
            String timeZone
    ) {
    }

    private final JdbcClient jdbc;

    public AuthRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // =========================================================
    // TENANT REGISTRATION
    // =========================================================

    public boolean tenantExists(String slug, String code) {
        return jdbc.sql("""
                        SELECT EXISTS (
                            SELECT 1
                            FROM tenants
                            WHERE slug = :slug
                               OR code = :code
                        )
                        """)
                .param("slug", slug)
                .param("code", code)
                .query(Boolean.class)
                .single();
    }

    public void insertTenant(
            UUID id,
            String slug,
            String code,
            String name,
            String email,
            String locale,
            OffsetDateTime trialEndsAt
    ) {
        jdbc.sql("""
                        INSERT INTO tenants (
                            id,
                            name,
                            slug,
                            code,
                            email,
                            status,
                            default_locale,
                            trial_ends_at,
                            created_at,
                            updated_at,
                            created_by,
                            updated_by,
                            version
                        )
                        VALUES (
                            :id,
                            :name,
                            :slug,
                            :code,
                            :email,
                            'ACTIVE',
                            :locale,
                            :trialEndsAt,
                            now(),
                            now(),
                            :actor,
                            :actor,
                            0
                        )
                        """)
                .param("id", id)
                .param("name", name)
                .param("slug", slug)
                .param("code", code)
                .param("email", email)
                .param("locale", locale)
                .param("trialEndsAt", trialEndsAt)
                .param("actor", SYSTEM_ACTOR)
                .update();
    }

    // =========================================================
    // ROLE
    // =========================================================

    public void insertRole(
            UUID id,
            UUID tenantId,
            String code,
            String name,
            String description
    ) {
        jdbc.sql("""
                        INSERT INTO roles (
                            id,
                            tenant_id,
                            code,
                            name,
                            description,
                            system_role,
                            created_at,
                            updated_at,
                            created_by,
                            updated_by,
                            version
                        )
                        VALUES (
                            :id,
                            :tenantId,
                            :code,
                            :name,
                            :description,
                            TRUE,
                            now(),
                            now(),
                            :actor,
                            :actor,
                            0
                        )
                        """)
                .param("id", id)
                .param("tenantId", tenantId)
                .param("code", code)
                .param("name", name)
                .param("description", description)
                .param("actor", SYSTEM_ACTOR)
                .update();
    }

    public void grantAllPermissions(UUID roleId) {
        jdbc.sql("""
                        INSERT INTO role_permissions (
                            role_id,
                            permission_id
                        )
                        SELECT
                            :roleId,
                            p.id
                        FROM permissions p
                        ON CONFLICT (role_id, permission_id)
                        DO NOTHING
                        """)
                .param("roleId", roleId)
                .update();
    }

    // =========================================================
    // USER
    // =========================================================

    public void insertUser(
            UUID id,
            UUID tenantId,
            String email,
            String firstName,
            String lastName,
            String passwordHash,
            String locale
    ) {
        jdbc.sql("""
                        INSERT INTO users (
                            id,
                            tenant_id,
                            email,
                            password_hash,
                            first_name,
                            last_name,
                            locale,
                            status,
                            failed_login_attempts,
                            created_at,
                            updated_at,
                            created_by,
                            updated_by,
                            version
                        )
                        VALUES (
                            :id,
                            :tenantId,
                            :email,
                            :passwordHash,
                            :firstName,
                            :lastName,
                            :locale,
                            'ACTIVE',
                            0,
                            now(),
                            now(),
                            :actor,
                            :actor,
                            0
                        )
                        """)
                .param("id", id)
                .param("tenantId", tenantId)
                .param("email", email)
                .param("passwordHash", passwordHash)
                .param("firstName", firstName)
                .param("lastName", lastName)
                .param("locale", locale)
                .param("actor", SYSTEM_ACTOR)
                .update();
    }

    // =========================================================
    // USER ROLE ASSIGNMENT
    // =========================================================

    public void assignRole(
            UUID id,
            UUID tenantId,
            UUID userId,
            UUID roleId
    ) {
        jdbc.sql("""
                        INSERT INTO user_roles (
                            id,
                            tenant_id,
                            user_id,
                            role_id,
                            created_at,
                            updated_at,
                            created_by,
                            updated_by,
                            version
                        )
                        VALUES (
                            :id,
                            :tenantId,
                            :userId,
                            :roleId,
                            now(),
                            now(),
                            :actor,
                            :actor,
                            0
                        )
                        """)
                .param("id", id)
                .param("tenantId", tenantId)
                .param("userId", userId)
                .param("roleId", roleId)
                .param("actor", SYSTEM_ACTOR)
                .update();
    }

    // =========================================================
    // AUDIT
    // =========================================================

    public void auditTenantRegistered(
            UUID tenantId,
            UUID adminUserId,
            String adminEmail,
            String slug
    ) {
        jdbc.sql("""
                        INSERT INTO audit_logs (
                            id,
                            tenant_id,
                            actor_user_id,
                            actor_label,
                            action,
                            entity_type,
                            entity_id,
                            new_values,
                            occurred_at
                        )
                        VALUES (
                            :id,
                            :tenantId,
                            :userId,
                            :email,
                            'CREATE',
                            'Tenant',
                            :entityId,
                            jsonb_build_object(
                                'slug', CAST(:slug AS text),
                                'adminEmail', CAST(:email AS text)
                            ),
                            now()
                        )
                        """)
                .param("id", UUID.randomUUID())
                .param("tenantId", tenantId)
                .param("userId", adminUserId)
                .param("email", adminEmail)
                .param("entityId", tenantId.toString())
                .param("slug", slug)
                .update();
    }

    // =========================================================
    // LOGIN
    // =========================================================

    public Optional<UserRow> findUser(
            String tenantSlug,
            String email
    ) {
        return jdbc.sql("""
                        SELECT
                            u.id,
                            u.tenant_id,
                            t.slug AS tenant_slug,
                            t.status AS tenant_status,
                            u.email,
                            u.first_name,
                            u.last_name,
                            u.password_hash,
                            u.status,
                            u.failed_login_attempts,
                            u.locked_until,
                            u.locale,
                            u.time_zone
                        FROM users u
                        JOIN tenants t
                          ON t.id = u.tenant_id
                        WHERE t.slug = :slug
                          AND lower(u.email) = :email
                        """)
                .param("slug", tenantSlug)
                .param("email", email)
                .query((rs, rowNum) -> new UserRow(
                        rs.getObject("id", UUID.class),
                        rs.getObject("tenant_id", UUID.class),
                        rs.getString("tenant_slug"),
                        rs.getString("tenant_status"),
                        rs.getString("email"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("password_hash"),
                        rs.getString("status"),
                        rs.getInt("failed_login_attempts"),
                        rs.getObject("locked_until", OffsetDateTime.class),
                        rs.getString("locale"),
                        rs.getString("time_zone")
                ))
                .optional();
    }

    public List<String> findRoleCodes(
            UUID tenantId,
            UUID userId
    ) {
        return jdbc.sql("""
                        SELECT DISTINCT r.code
                        FROM user_roles ur
                        JOIN roles r
                          ON r.id = ur.role_id
                         AND r.tenant_id = ur.tenant_id
                        WHERE ur.tenant_id = :tenantId
                          AND ur.user_id = :userId
                        ORDER BY r.code
                        """)
                .param("tenantId", tenantId)
                .param("userId", userId)
                .query(String.class)
                .list();
    }

    public List<String> findPermissionCodes(
            UUID tenantId,
            UUID userId
    ) {
        return jdbc.sql("""
                        SELECT DISTINCT p.code
                        FROM user_roles ur
                        JOIN role_permissions rp
                          ON rp.role_id = ur.role_id
                        JOIN permissions p
                          ON p.id = rp.permission_id
                        WHERE ur.tenant_id = :tenantId
                          AND ur.user_id = :userId
                        ORDER BY p.code
                        """)
                .param("tenantId", tenantId)
                .param("userId", userId)
                .query(String.class)
                .list();
    }

    // =========================================================
    // FAILED LOGIN ATTEMPT
    // =========================================================

    public void recordFailedAttempt(
            UUID userId,
            int maxAttempts,
            int lockMinutes
    ) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        if (maxAttempts <= 0 || lockMinutes <= 0) {
            throw new IllegalArgumentException("Invalid lock configuration");
        }

        jdbc.sql("""
            UPDATE users
            SET failed_login_attempts =
                    CASE
                        WHEN COALESCE(failed_login_attempts, 0) + 1 >= :max
                        THEN 0
                        ELSE COALESCE(failed_login_attempts, 0) + 1
                    END,
                locked_until =
                    CASE
                        WHEN COALESCE(failed_login_attempts, 0) + 1 >= :max
                        THEN now() + (:lockMinutes * INTERVAL '1 minute')
                        ELSE locked_until
                    END,
                updated_at = now(),
                version = version + 1
            WHERE id = :id
            """)
                .param("max", maxAttempts)
                .param("lockMinutes", lockMinutes)
                .param("id", userId)
                .update();
    }

    // =========================================================
    // SUCCESSFUL LOGIN
    // =========================================================

    public void markLoggedIn(UUID userId) {
        jdbc.sql("""
                        UPDATE users
                        SET failed_login_attempts = 0,
                            locked_until = NULL,
                            last_login_at = now(),
                            updated_at = now(),
                            version = version + 1
                        WHERE id = :id
                        """)
                .param("id", userId)
                .update();
    }
}
