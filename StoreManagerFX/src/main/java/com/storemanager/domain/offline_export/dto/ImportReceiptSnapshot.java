package com.storemanager.domain.offline_export.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ImportReceiptSnapshot(Long sourceId, String supplierName, BigDecimal totalCost,
                                    Long sourceCreatedByUserId, LocalDateTime createdAt) {
}
