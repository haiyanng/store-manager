package com.customershopfx.order.model;

import java.math.BigDecimal;

public record OrderItem(Long id, Long productId, String productName, int quantity, BigDecimal unitPrice,
                        BigDecimal subtotal) {
}
