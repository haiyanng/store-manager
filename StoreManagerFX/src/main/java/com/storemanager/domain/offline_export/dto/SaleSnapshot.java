package com.storemanager.domain.offline_export.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SaleSnapshot(
        Long sourceId,
        Long sourceCreatedByUserId,
        BigDecimal totalAmount,
        LocalDateTime createdAt,
        BigDecimal amountReceived,
        BigDecimal changeAmount
) {
    public SaleSnapshot(Long sourceId, Long sourceCreatedByUserId, BigDecimal totalAmount, LocalDateTime createdAt) {
        this(sourceId, sourceCreatedByUserId, totalAmount, createdAt, null, null);
    }
}
