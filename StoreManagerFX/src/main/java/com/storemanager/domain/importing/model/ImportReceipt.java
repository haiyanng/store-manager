package com.storemanager.domain.importing.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ImportReceipt {

    private Long id;

    private String supplierName;

    private BigDecimal totalCost;

    private Long createdByUserId;

    private LocalDateTime createdAt;

    public ImportReceipt() {
    }

    public Long getId() {
        return id;
    }

    public void setId(
            Long id
    ) {
        this.id = id;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(
            String supplierName
    ) {
        this.supplierName = supplierName;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(
            BigDecimal totalCost
    ) {
        this.totalCost = totalCost;
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
