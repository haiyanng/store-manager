package com.storeapi.cart.dto;

import java.math.BigDecimal;

public record CartItemDto(Long id, Long productId, String productName, BigDecimal unitPrice,
                          int quantity, BigDecimal subtotal, String imagePath) {
}
