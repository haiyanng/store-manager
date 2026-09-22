package com.storemanager.domain.sale.model;

import com.storemanager.domain.product.model.Product;

import java.math.BigDecimal;

public class SaleCartItem {

    private Product product;

    private int quantity;

    public SaleCartItem(
            Product product,
            int quantity
    ) {

        this.product = product;
        this.quantity = quantity;
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

    public BigDecimal getUnitPrice() {

        if (product == null || product.getBasePrice() == null) {
            return BigDecimal.ZERO;
        }

        return product.getBasePrice();
    }

    public BigDecimal getSubtotal() {

        return getUnitPrice().multiply(
                BigDecimal.valueOf(quantity)
        );
    }
}
