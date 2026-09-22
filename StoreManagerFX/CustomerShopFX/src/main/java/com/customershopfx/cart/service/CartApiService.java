package com.customershopfx.cart.service;

import com.customershopfx.app.AppContext;
import com.customershopfx.common.api.ApiClient;
import com.customershopfx.cart.model.Cart;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Map;

public class CartApiService {
    private final ApiClient api = AppContext.apiClient();

    public Cart cart() {
        return api.get("/api/cart", new TypeReference<>() {});
    }

    public Cart add(Long productId, int quantity) {
        return api.post("/api/cart/items", Map.of("productId", productId, "quantity", quantity), new TypeReference<>() {});
    }

    public Cart update(Long itemId, int quantity) {
        return api.put("/api/cart/items/" + itemId, Map.of("quantity", quantity), new TypeReference<>() {});
    }

    public Cart remove(Long itemId) {
        return api.delete("/api/cart/items/" + itemId, new TypeReference<>() {});
    }

    public Cart clear() {
        return api.delete("/api/cart/clear", new TypeReference<>() {});
    }
}
