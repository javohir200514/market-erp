package com.universal.erp.platform.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Read methods always take tenantId explicitly - AuditLog is not covered by @TenantId. */
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByTenantIdOrderByOccurredAtDesc(UUID tenantId, Pageable pageable);

    Page<AuditLog> findByTenantIdAndEntityTypeAndEntityIdOrderByOccurredAtDesc(
            UUID tenantId, String entityType, String entityId, Pageable pageable);

    Page<AuditLog> findByTenantIdAndActorUserIdOrderByOccurredAtDesc(
            UUID tenantId, UUID actorUserId, Pageable pageable);
}
