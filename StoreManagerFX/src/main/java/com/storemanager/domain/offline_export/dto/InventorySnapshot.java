package com.storemanager.domain.offline_export.dto;

import java.time.LocalDateTime;

public record InventorySnapshot(
        Long sourceId,
        Long sourceProductId,
        int quantity,
        LocalDateTime updatedAt
) {
}
