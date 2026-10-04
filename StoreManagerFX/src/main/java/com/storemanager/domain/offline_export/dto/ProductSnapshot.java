package com.storemanager.domain.offline_export.dto;

import java.math.BigDecimal;

public record ProductSnapshot(
        Long sourceId,
        String name,
        String sku,
        Long sourceCategoryId,
        BigDecimal basePrice,
        String unit,
        boolean active,
        String imagePath
) {
}
