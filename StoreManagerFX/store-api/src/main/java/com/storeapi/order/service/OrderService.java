package com.storeapi.order.service;

import com.storeapi.cart.dto.CartDto;
import com.storeapi.cart.dto.CartItemDto;
import com.storeapi.cart.service.CartService;
import com.storeapi.order.dto.CheckoutRequest;
import com.storeapi.order.dto.OrderDto;
import com.storeapi.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final OrderRepository orders;
    private final CartService carts;

    public OrderService(OrderRepository orders, CartService carts) {
        this.orders = orders;
        this.carts = carts;
    }

    @Transactional
    public OrderDto place(Long customerId, CheckoutRequest request) {
        java.util.List<String> inactiveProducts =
                carts.inactiveProductNames(customerId);
        if (!inactiveProducts.isEmpty()) {
            carts.removeInactiveItems(customerId);
            orders.addAuditEvent(
                    customerId,
                    "CHECKOUT_FAILED",
                    false,
                    "Product is inactive and cannot be checked out.",
                    "{\"inactive_products\":" + inactiveProducts.size() + "}"
            );
            throw new IllegalStateException(
                    "Product is inactive and cannot be checked out."
            );
        }

        CartDto cart = carts.cart(customerId);
        if (cart.items().isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }
        Long orderId = orders.create(customerId, request, cart.total());
        for (CartItemDto item : cart.items()) {
            orders.addItem(orderId, item.productId(), item.productName(), item.quantity(), item.unitPrice(), item.subtotal());
        }
        orders.addHistory(
                orderId,
                "CREATE",
                "NONE",
                "PENDING",
                "Order placed by customer"
        );
        carts.clear(customerId);
        return orders.find(customerId, orderId).orElseThrow();
    }

    public java.util.List<OrderDto> myOrders(Long customerId) {
        return orders.myOrders(customerId);
    }

    public OrderDto detail(Long customerId, Long orderId) {
        return orders.find(customerId, orderId).orElseThrow();
    }

    @Transactional
    public OrderDto cancel(Long customerId, Long orderId) {
        OrderDto before =
                orders.find(customerId, orderId)
                        .orElseThrow();
        if (!orders.cancel(customerId, orderId)) {
            throw new IllegalStateException("Only pending orders can be cancelled");
        }
        orders.addHistory(
                orderId,
                "CANCEL",
                before.status(),
                "CANCELLED",
                "Cancelled by customer"
        );
        return orders.find(customerId, orderId).orElseThrow();
    }
}
