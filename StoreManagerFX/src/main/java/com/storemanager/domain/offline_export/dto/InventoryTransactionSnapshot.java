package com.storemanager.domain.offline_export.dto;

import com.storemanager.domain.inventory.model.InventoryTransactionType;
import java.time.LocalDateTime;

public record InventoryTransactionSnapshot(Long sourceId, Long sourceProductId, InventoryTransactionType type,
                                          int quantity, String reason, Long sourceCreatedByUserId,
                                          LocalDateTime createdAt) {
}
