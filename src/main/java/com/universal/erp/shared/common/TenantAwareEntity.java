package com.universal.erp.shared.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

/**
 * Base class for every tenant-owned record.
 * <p>
 * {@link TenantId} makes Hibernate set tenant_id on insert from the current tenant
 * identifier and append "tenant_id = ?" to every HQL/Criteria/derived query, so a
 * repository can never return another tenant's rows. Native SQL is NOT filtered;
 * PostgreSQL Row-Level Security (planned) is the second line of defence for that.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class TenantAwareEntity extends BaseEntity {

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;
}
