package com.storemanager.domain.offline_export.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PayrollSnapshot(
        Long sourceId,
        Long sourceEmployeeId,
        int month,
        int year,
        BigDecimal totalHours,
        BigDecimal hourlyRateSnapshot,
        BigDecimal totalSalary,
        LocalDateTime generatedAt,
        Long sourceGeneratedByUserId
) {
}
