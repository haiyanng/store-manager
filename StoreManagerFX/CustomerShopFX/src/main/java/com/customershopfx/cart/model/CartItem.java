package com.customershopfx.cart.model;

import java.math.BigDecimal;

public record CartItem(Long id, Long productId, String productName, BigDecimal unitPrice, int quantity,
                       BigDecimal subtotal, String imagePath) {
}
