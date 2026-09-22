package com.storeapi.cart.controller;

import com.storeapi.cart.dto.CartDto;
import com.storeapi.cart.dto.CartItemRequest;
import com.storeapi.cart.dto.QuantityRequest;
import com.storeapi.cart.service.CartService;
import com.storeapi.security.CustomerPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final CartService carts;

    public CartController(CartService carts) {
        this.carts = carts;
    }

    @GetMapping
    CartDto cart(@AuthenticationPrincipal CustomerPrincipal principal) {
        return carts.cart(principal.id());
    }

    @PostMapping("/items")
    CartDto add(@AuthenticationPrincipal CustomerPrincipal principal, @Valid @RequestBody CartItemRequest request) {
        return carts.add(principal.id(), request);
    }

    @PutMapping("/items/{id}")
    CartDto update(@AuthenticationPrincipal CustomerPrincipal principal, @PathVariable Long id,
                   @Valid @RequestBody QuantityRequest request) {
        return carts.update(principal.id(), id, request);
    }

    @DeleteMapping("/items/{id}")
    CartDto remove(@AuthenticationPrincipal CustomerPrincipal principal, @PathVariable Long id) {
        return carts.remove(principal.id(), id);
    }

    @DeleteMapping("/clear")
    CartDto clear(@AuthenticationPrincipal CustomerPrincipal principal) {
        return carts.clear(principal.id());
    }
}
