package com.universal.erp.platform.tenant;

import com.universal.erp.platform.permissions.Role;
import com.universal.erp.platform.permissions.TenantRoleSeeder;
import com.universal.erp.platform.permissions.UserRole;
import com.universal.erp.platform.permissions.UserRoleRepository;
import com.universal.erp.platform.user.entity.User;
import com.universal.erp.platform.user.repository.UserRepository;
import com.universal.erp.platform.user.enums.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

/**
 * Transactional steps of tenant registration. Called only by {@link TenantRegistrationService}.
 * Methods are on a separate bean so that the Spring transaction proxy is applied to each of them.
 */
@Service
public class TenantRegistrationWorker {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final TenantRoleSeeder roleSeeder;

    public TenantRegistrationWorker(TenantRepository tenantRepository, UserRepository userRepository,
                                    UserRoleRepository userRoleRepository, TenantRoleSeeder roleSeeder) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleSeeder = roleSeeder;
    }

    /** Step 1. The tenants table is not tenant-owned, so no TenantContext is needed here. */
    @Transactional
    public UUID createPendingTenant(TenantRegistrationDto.Request request, String slug, String normalizedEmail) {
        Tenant tenant = new Tenant();
        tenant.setName(request.tenantName().trim());
        tenant.setSlug(slug);
        tenant.setCode(slug.toUpperCase(Locale.ROOT));
        tenant.setEmail(normalizedEmail);
        tenant.setStatus(TenantStatus.PENDING);
        tenant.setDefaultLocale(defaultIfBlank(request.locale(), "uz"));
        tenant.setDefaultTimeZone(defaultIfBlank(request.timeZone(), "Asia/Tashkent"));
        return tenantRepository.save(tenant).getId();
    }

    /**
     * Step 2. TenantContext MUST already be bound to {@code tenantId} (done by the caller)
     * before this transaction starts. Admin, default roles and the admin grant are atomic.
     */
    @Transactional
    public TenantRegistrationDto.Response provisionTenant(UUID tenantId, TenantRegistrationDto.Request request,
                                                          String normalizedEmail, String passwordHash) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found"));

        User admin = new User();
        admin.setEmail(normalizedEmail);
        admin.setPasswordHash(passwordHash);
        admin.setFirstName(request.adminFirstName().trim());
        admin.setLastName(request.adminLastName().trim());
        admin.setLocale(tenant.getDefaultLocale());
        admin.setTimeZone(tenant.getDefaultTimeZone());
        admin.setStatus(UserStatus.ACTIVE);
        userRepository.save(admin);

        Role adminRole = roleSeeder.seedDefaultRoles();

        UserRole grant = new UserRole();
        grant.setUser(admin);
        grant.setRole(adminRole);
        userRoleRepository.save(grant);

        tenant.activate();

        return new TenantRegistrationDto.Response(tenant.getId(), tenant.getName(), tenant.getSlug(),
                admin.getId(), admin.getEmail(), tenant.getStatus(), admin.getStatus());
    }

    /** Compensation for a failed step 2. Only a PENDING tenant (no children) may be removed. */
    @Transactional
    public void discardPendingTenant(UUID tenantId) {
        tenantRepository.findById(tenantId)
                .filter(t -> t.getStatus() == TenantStatus.PENDING)
                .ifPresent(tenantRepository::delete);
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}