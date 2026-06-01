package com.storemanager.domain.onlineorder.model;

import java.util.ArrayList;
import java.util.List;

public class OnlineOrderDetail {

    private OnlineOrder order;

    private List<OnlineOrderItem> items = new ArrayList<>();

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
}
