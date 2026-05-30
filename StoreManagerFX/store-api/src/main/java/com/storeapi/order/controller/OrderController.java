package com.storeapi.order.controller;

import com.storeapi.order.dto.CheckoutRequest;
import com.storeapi.order.dto.OrderDto;
import com.storeapi.order.service.OrderService;
import com.storeapi.security.CustomerPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @PostMapping
    OrderDto place(@AuthenticationPrincipal CustomerPrincipal principal, @Valid @RequestBody CheckoutRequest request) {
        return orders.place(principal.id(), request);
    }

    @GetMapping("/my")
    List<OrderDto> my(@AuthenticationPrincipal CustomerPrincipal principal) {
        return orders.myOrders(principal.id());
    }

    @GetMapping("/{id}")
    OrderDto detail(@AuthenticationPrincipal CustomerPrincipal principal, @PathVariable Long id) {
        return orders.detail(principal.id(), id);
    }

    @PutMapping("/{id}/cancel")
    OrderDto cancel(@AuthenticationPrincipal CustomerPrincipal principal, @PathVariable Long id) {
        return orders.cancel(principal.id(), id);
    }
}
