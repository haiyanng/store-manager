package com.storeapi.order.dto;

import jakarta.validation.constraints.NotBlank;

public record CheckoutRequest(@NotBlank String recipientName, @NotBlank String phone,
                              @NotBlank String shippingAddress, String paymentMethod) {
}
