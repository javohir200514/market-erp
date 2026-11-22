package com.universal.erp.platform.tenant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class TenantService {
    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    /** Tenant lookup is a platform-level operation and intentionally runs without tenant filtering. */
    @Transactional(readOnly = true)
    public Tenant findBySlug(String slug) {
        return tenantRepository.findBySlug(slug.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found"));
    }

    @Transactional
    public Tenant activate(UUID tenantId) {
        Tenant tenant = requireTenant(tenantId);
        tenant.setStatus(TenantStatus.ACTIVE);
        return tenant;
    }

    @Transactional
    public Tenant suspend(UUID tenantId) {
        Tenant tenant = requireTenant(tenantId);
        tenant.setStatus(TenantStatus.SUSPENDED);
        return tenant;
    }

    private Tenant requireTenant(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found"));
    }
}
