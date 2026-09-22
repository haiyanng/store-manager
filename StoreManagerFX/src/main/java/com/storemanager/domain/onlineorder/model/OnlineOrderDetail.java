package com.storemanager.domain.onlineorder.model;

import java.util.ArrayList;
import java.util.List;

public class OnlineOrderDetail {

    private OnlineOrder order;

    private List<OnlineOrderItem> items = new ArrayList<>();

    private List<OnlineOrderHistoryEntry> history = new ArrayList<>();

    public OnlineOrderDetail() {
    }

    public OnlineOrder getOrder() {
        return order;
    }

    public void setOrder(OnlineOrder order) {
        this.order = order;
    }

    public List<OnlineOrderItem> getItems() {
        return items;
    }

    public void setItems(List<OnlineOrderItem> items) {
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
    }

    public List<OnlineOrderHistoryEntry> getHistory() {
        return history;
    }

    public void setHistory(List<OnlineOrderHistoryEntry> history) {
        this.history = history == null ? new ArrayList<>() : new ArrayList<>(history);
    }
}
