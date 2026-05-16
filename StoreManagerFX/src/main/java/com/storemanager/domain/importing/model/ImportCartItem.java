package com.storemanager.domain.importing.model;

import com.storemanager.domain.product.model.Product;

import java.math.BigDecimal;

public class ImportCartItem {

    private Product product;

    private int quantity;

    private BigDecimal unitCost;

    public ImportCartItem(
            Product product,
            int quantity,
            BigDecimal unitCost
    ) {

        this.product = product;
        this.quantity = quantity;
        this.unitCost = unitCost;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(
            Product product
    ) {
        this.product = product;
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
        return unitCost == null
                ? BigDecimal.ZERO
                : unitCost;
    }

    public void setUnitCost(
            BigDecimal unitCost
    ) {
        this.unitCost = unitCost;
    }

    public BigDecimal getSubtotal() {

        return getUnitCost().multiply(
                BigDecimal.valueOf(quantity)
        );
    }
}
