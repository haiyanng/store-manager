package com.storemanager.domain.dashboard.model;

public class DashboardInventoryAlertRow {

    private String productName;

    private long quantity;

    private String alertText;

    public DashboardInventoryAlertRow() {
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public long getQuantity() {
        return quantity;
    }

    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }

    public String getAlertText() {
        return alertText;
    }

    public void setAlertText(String alertText) {
        this.alertText = alertText;
    }
}
