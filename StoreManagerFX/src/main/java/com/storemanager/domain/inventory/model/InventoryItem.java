package com.storemanager.domain.inventory.model;

import java.time.LocalDateTime;
import java.time.LocalDate;

public class InventoryItem {

    private Long id;

    private Long productId;

    private int quantity;

    private LocalDateTime updatedAt;

    private LocalDate nearestExpiryDate;
    private String expiryStatus = "N/A";

    public LocalDate getNearestExpiryDate() { return nearestExpiryDate; }

    public void setNearestExpiryDate(LocalDate value) { nearestExpiryDate = value; }

    public String getExpiryStatus() { return expiryStatus; }

    public void setExpiryStatus(String value) { expiryStatus = value; }

    public InventoryItem() {
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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(
            int quantity
    ) {
        this.quantity = quantity;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(
            LocalDateTime updatedAt
    ) {
        this.updatedAt = updatedAt;
    }
}
