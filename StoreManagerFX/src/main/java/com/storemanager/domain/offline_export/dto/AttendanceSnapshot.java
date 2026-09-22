package com.storemanager.domain.offline_export.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AttendanceSnapshot(
        Long sourceId,
        Long sourceEmployeeId,
        Long sourceBranchId,
        LocalDateTime checkInTime,
        LocalDateTime checkOutTime,
        BigDecimal workedHours,
        Long sourceCreatedByUserId,
        LocalDateTime createdAt
) {
}
