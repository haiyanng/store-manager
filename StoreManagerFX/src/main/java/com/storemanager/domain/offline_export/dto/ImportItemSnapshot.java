package com.storemanager.domain.offline_export.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ImportItemSnapshot(Long sourceId, Long sourceImportReceiptId, Long sourceProductId,
                                 int quantity, BigDecimal unitCost, BigDecimal subtotal, LocalDate expiryDate) {
}
