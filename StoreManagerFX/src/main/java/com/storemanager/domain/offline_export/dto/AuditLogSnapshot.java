package com.storemanager.domain.offline_export.dto;

import java.time.LocalDateTime;

public record AuditLogSnapshot(
        Long sourceId,
        Long sourceUserId,
        String action,
        String entityType,
        Long sourceEntityId,
        String details,
        Long sourceBranchId,
        LocalDateTime createdAt
) {
}
