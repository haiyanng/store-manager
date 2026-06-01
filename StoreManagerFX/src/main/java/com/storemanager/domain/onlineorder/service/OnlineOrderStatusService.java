package com.storemanager.domain.onlineorder.service;

import com.storemanager.domain.onlineorder.model.OnlineOrderStatus;

public class OnlineOrderStatusService {

    public void validateTransition(String currentStatus, String nextStatus) {
        if (!OnlineOrderStatus.isKnown(currentStatus)) {
            throw new IllegalArgumentException("Unknown current status: " + currentStatus);
        }
        if (!OnlineOrderStatus.isKnown(nextStatus)) {
            throw new IllegalArgumentException("Unknown target status: " + nextStatus);
        }
        if (!OnlineOrderStatus.canTransition(currentStatus, nextStatus)) {
            throw new IllegalArgumentException("Invalid order status transition");
        }
    }
}
