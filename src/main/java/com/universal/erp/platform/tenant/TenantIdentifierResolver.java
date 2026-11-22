
package com.universal.erp.platform.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TenantIdentifierResolver
        implements CurrentTenantIdentifierResolver<UUID> {

    /**
     * Tenant hali aniqlanmagan holat uchun rezerv ID.
     * Bu ID hech qachon haqiqiy tenant sifatida ishlatilmasligi kerak.
     */
    private static final UUID DEFAULT_TENANT_ID =
            new UUID(0L, 0L);

    @Override
    public UUID resolveCurrentTenantIdentifier() {

        UUID tenantId = TenantContext.getTenantId();

        return tenantId != null
                ? tenantId
                : DEFAULT_TENANT_ID;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
