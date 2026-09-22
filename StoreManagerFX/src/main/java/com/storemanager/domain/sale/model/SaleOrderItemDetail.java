package com.storemanager.domain.sale.model;

import java.math.BigDecimal;

public class SaleOrderItemDetail {

    private Long orderItemId;

    private Long productId;

    private String productName;

    private String sku;

    private int quantity;

    private BigDecimal unitPrice;

    private BigDecimal subtotal;

    public SaleOrderItemDetail() {
    }

    public Long getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(
            Long orderItemId
    ) {
        this.orderItemId = orderItemId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(
            Long productId
    ) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(
            String productName
    ) {
        this.productName = productName;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(
            String sku
    ) {
        this.sku = sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(
            int quantity
    ) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(
            BigDecimal unitPrice
    ) {
        this.unitPrice = unitPrice;
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
