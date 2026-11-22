package com.universal.erp.platform.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditLogService {
    private final AuditLogRepository repository;

    public AuditLogService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditAction action, UUID tenantId, UUID actorUserId, String actorLabel,
                       String entityType, String entityId, String ipAddress) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setTenantId(tenantId);
        log.setActorUserId(actorUserId);
        log.setActorLabel(truncate(actorLabel, 254));
        log.setEntityType(entityType);
        log.setEntityId(truncate(entityId, 64));
        log.setIpAddress(truncate(ipAddress, 45));
        repository.save(log);
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}