package com.storemanager.domain.dashboard.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DashboardOnlineOrderSnapshot {

    private long totalOrders;

    private long pendingOrders;

    private long confirmedOrders;

    private long deliveringOrders;

    private long deliveredOrders;

    private long cancelledOrders;

    private BigDecimal totalRevenue = BigDecimal.ZERO;

    private BigDecimal revenueToday = BigDecimal.ZERO;

    private long ordersToday;

    private List<DashboardOnlineOrderRow> recentOrders = new ArrayList<>();

    public DashboardOnlineOrderSnapshot() {
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public long getPendingOrders() {
        return pendingOrders;
    }

    public void setPendingOrders(long pendingOrders) {
        this.pendingOrders = pendingOrders;
    }

    public long getConfirmedOrders() {
        return confirmedOrders;
    }

    public void setConfirmedOrders(long confirmedOrders) {
        this.confirmedOrders = confirmedOrders;
    }

    public long getDeliveringOrders() {
        return deliveringOrders;
    }

    public void setDeliveringOrders(long deliveringOrders) {
        this.deliveringOrders = deliveringOrders;
    }

    public long getDeliveredOrders() {
        return deliveredOrders;
    }

    public void setDeliveredOrders(long deliveredOrders) {
        this.deliveredOrders = deliveredOrders;
    }

    public long getCancelledOrders() {
        return cancelledOrders;
    }

    public void setCancelledOrders(long cancelledOrders) {
        this.cancelledOrders = cancelledOrders;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue == null ? BigDecimal.ZERO : totalRevenue;
    }

    public BigDecimal getRevenueToday() {
        return revenueToday;
    }

    public void setRevenueToday(BigDecimal revenueToday) {
        this.revenueToday = revenueToday == null ? BigDecimal.ZERO : revenueToday;
    }

    public long getOrdersToday() {
        return ordersToday;
    }

    public void setOrdersToday(long ordersToday) {
        this.ordersToday = ordersToday;
    }

    public List<DashboardOnlineOrderRow> getRecentOrders() {
        return recentOrders;
    }

    public void setRecentOrders(List<DashboardOnlineOrderRow> recentOrders) {
        this.recentOrders = recentOrders == null ? new ArrayList<>() : new ArrayList<>(recentOrders);
    }
}
