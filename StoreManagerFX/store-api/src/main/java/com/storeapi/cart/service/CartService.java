package com.storeapi.cart.service;

import com.storeapi.cart.dto.CartDto;
import com.storeapi.cart.dto.CartItemRequest;
import com.storeapi.cart.dto.QuantityRequest;
import com.storeapi.cart.repository.CartRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartService {
    private final CartRepository carts;

    public CartService(CartRepository carts) {
        this.carts = carts;
    }

    public CartDto cart(Long customerId) {
        return carts.cart(customerId);
    }

    public CartDto add(Long customerId, CartItemRequest request) {
        carts.addItem(customerId, request.productId(), request.quantity());
        return carts.cart(customerId);
    }

    public List<String> inactiveProductNames(Long customerId) {
        return carts.inactiveProductNames(customerId);
    }

    public void removeInactiveItems(Long customerId) {
        carts.removeInactiveItems(customerId);
    }

    public CartDto update(Long customerId, Long itemId, QuantityRequest request) {
        carts.updateItem(customerId, itemId, request.quantity());
        return carts.cart(customerId);
    }

    public CartDto remove(Long customerId, Long itemId) {
        carts.removeItem(customerId, itemId);
        return carts.cart(customerId);
    }

    public CartDto clear(Long customerId) {
        carts.clear(customerId);
        return carts.cart(customerId);
    }
}
