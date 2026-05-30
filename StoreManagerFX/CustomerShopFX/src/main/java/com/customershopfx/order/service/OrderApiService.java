package com.customershopfx.order.service;

import com.customershopfx.app.AppContext;
import com.customershopfx.common.api.ApiClient;
import com.customershopfx.order.model.Order;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Map;

public class OrderApiService {
    private final ApiClient api = AppContext.apiClient();

    public Order create(String recipientName, String phone, String shippingAddress, String paymentMethod) {
        return api.post("/api/orders", Map.of(
                "recipientName", recipientName,
                "phone", phone,
                "shippingAddress", shippingAddress,
                "paymentMethod", paymentMethod == null ? "Cash on delivery" : paymentMethod), new TypeReference<>() {});
    }

    public List<Order> myOrders() {
        return api.get("/api/orders/my", new TypeReference<>() {});
    }

    public Order detail(Long id) {
        return api.get("/api/orders/" + id, new TypeReference<>() {});
    }

    public Order cancel(Long id) {
        return api.put("/api/orders/" + id + "/cancel", Map.of(), new TypeReference<>() {});
    }
}
