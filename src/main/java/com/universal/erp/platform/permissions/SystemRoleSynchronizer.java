package com.universal.erp.platform.permissions;

import com.universal.erp.platform.tenant.Tenant;
import com.universal.erp.platform.tenant.TenantContext;
import com.universal.erp.platform.tenant.TenantRepository;
import com.universal.erp.platform.tenant.TenantStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * On application start, aligns the system roles of EVERY existing tenant with the current permission catalog.
 *
 * Why: new modules add permissions through Flyway (e.g. V4__seed_crm_permissions.sql). Tenants registered
 * earlier would otherwise never receive them (their TENANT_ADMIN would be missing the new permissions).
 *
 * Failure of one tenant never blocks start-up or the other tenants. Disable with
 * {@code erp.roles.sync-on-startup=false}.
 */
@Component
@ConditionalOnProperty(name = "erp.roles.sync-on-startup", havingValue = "true", matchIfMissing = true)
public class SystemRoleSynchronizer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SystemRoleSynchronizer.class);

    private final TenantRepository tenantRepository;
    private final TenantRoleSeeder seeder;

    public SystemRoleSynchronizer(TenantRepository tenantRepository, TenantRoleSeeder seeder) {
        this.tenantRepository = tenantRepository;
        this.seeder = seeder;
    }

    @Override
    public void run(ApplicationArguments args) {
        int synced = 0;
        int failed = 0;
        for (Tenant tenant : tenantRepository.findAll()) {
            if (tenant.getStatus() == TenantStatus.PENDING) {
                continue; // registration still in progress; it seeds its own roles
            }
            try {
                TenantContext.runAs(tenant.getId(), seeder::seedDefaultRoles);
                synced++;
            } catch (RuntimeException ex) {
                failed++;
                log.error("System role sync failed for tenant {}", tenant.getSlug(), ex);
            }
        }
        log.info("System role sync finished: {} tenant(s) synced, {} failed", synced, failed);
    }
}