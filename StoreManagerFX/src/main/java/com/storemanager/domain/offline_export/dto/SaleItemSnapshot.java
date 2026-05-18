package com.storemanager.domain.offline_export.dto;

import java.math.BigDecimal;

public record SaleItemSnapshot(
        Long sourceId,
        Long sourceOrderId,
        Long sourceProductId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
}
