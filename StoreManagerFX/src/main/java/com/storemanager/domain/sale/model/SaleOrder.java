package com.storemanager.domain.sale.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SaleOrder {

    private Long id;

    private Long createdByUserId;

    private BigDecimal totalAmount;

    private LocalDateTime createdAt;

    public SaleOrder() {
    }

    public Long getId() {
        return id;
    }

    public void setId(
            Long id
    ) {
        this.id = id;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(
            Long createdByUserId
    ) {
        this.createdByUserId = createdByUserId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(
            BigDecimal totalAmount
    ) {
        this.totalAmount = totalAmount;
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
