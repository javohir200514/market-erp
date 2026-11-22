package com.universal.erp.platform.audit;

import java.time.Instant;
import java.util.UUID;

public final class AuditLogDto {

    private AuditLogDto() {
    }

    public record Response(
            UUID id, UUID actorUserId, String actorLabel, AuditAction action,
            String entityType, String entityId, String oldValues, String newValues,
            String ipAddress, Instant occurredAt) {

        public static Response from(AuditLog a) {
            return new Response(a.getId(), a.getActorUserId(), a.getActorLabel(), a.getAction(),
                    a.getEntityType(), a.getEntityId(), a.getOldValues(), a.getNewValues(),
                    a.getIpAddress(), a.getOccurredAt());
        }
    }
}
