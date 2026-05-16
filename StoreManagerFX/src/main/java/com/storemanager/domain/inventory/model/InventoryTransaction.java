package com.storemanager.domain.inventory.model;

import java.time.LocalDateTime;

public class InventoryTransaction {

    private Long id;

    private Long productId;

    private InventoryTransactionType type;

    private int quantity;

    private String reason;

    private Long createdByUserId;

    private LocalDateTime createdAt;

    public InventoryTransaction() {
    }

    public Long getId() {
        return id;
    }

    public void setId(
            Long id
    ) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(
            Long productId
    ) {
        this.productId = productId;
    }

    public InventoryTransactionType getType() {
        return type;
    }

    public void setType(
            InventoryTransactionType type
    ) {
        this.type = type;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(
            int quantity
    ) {
        this.quantity = quantity;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(
            String reason
    ) {
        this.reason = reason;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(
            Long createdByUserId
    ) {
        this.createdByUserId = createdByUserId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(
            LocalDateTime createdAt
    ) {
        this.createdAt = createdAt;
    }
}
