package com.storemanager.domain.onlineorder.service;

import com.storemanager.domain.onlineorder.dao.OnlineOrderDAO;
import com.storemanager.domain.onlineorder.dao.OnlineOrderItemDAO;
import com.storemanager.domain.onlineorder.model.OnlineOrderDetail;
import com.storemanager.domain.onlineorder.model.OnlineOrderSummary;

import java.util.List;
import java.util.NoSuchElementException;

public class OnlineOrderService {

    private final OnlineOrderDAO onlineOrderDAO = new OnlineOrderDAO();

    private final OnlineOrderItemDAO onlineOrderItemDAO = new OnlineOrderItemDAO();

    public List<OnlineOrderSummary> loadAllOrders() {
        return onlineOrderDAO.findAll();
    }

    public OnlineOrderDetail loadOrderDetails(Long orderId) {
        OnlineOrderDetail detail = new OnlineOrderDetail();
        detail.setOrder(
                onlineOrderDAO.findById(orderId)
                        .orElseThrow(() -> new NoSuchElementException("Online order not found"))
        );
        detail.setItems(onlineOrderItemDAO.findItemsByOrderId(orderId));
        detail.setHistory(onlineOrderDAO.findHistoryByOrderId(orderId));
        return detail;
    }
}
