package com.storemanager.domain.importing.model;

import java.math.BigDecimal;

public class ImportItem {

    private Long id;

    private Long importReceiptId;

    private Long productId;

    private int quantity;

    private BigDecimal unitCost;

    private BigDecimal subtotal;

    public ImportItem() {
    }

    public Long getId() {
        return id;
    }

    public void setId(
            Long id
    ) {
        this.id = id;
    }

    public Long getImportReceiptId() {
        return importReceiptId;
    }

    public void setImportReceiptId(
            Long importReceiptId
    ) {
        this.importReceiptId = importReceiptId;
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

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(
            BigDecimal unitCost
    ) {
        this.unitCost = unitCost;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(
            BigDecimal subtotal
    ) {
        this.subtotal = subtotal;
    }
}
