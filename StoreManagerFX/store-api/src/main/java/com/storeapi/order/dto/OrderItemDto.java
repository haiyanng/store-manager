package com.storeapi.order.dto;

import java.math.BigDecimal;

public record OrderItemDto(Long id, Long productId, String productName, int quantity,
                           BigDecimal unitPrice, BigDecimal subtotal) {
}
