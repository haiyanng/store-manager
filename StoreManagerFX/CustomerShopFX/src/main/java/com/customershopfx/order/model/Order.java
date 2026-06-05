package com.customershopfx.order.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record Order(Long id, String status, BigDecimal totalAmount, String recipientName, String phone,
                    String shippingAddress, String paymentMethod, LocalDateTime createdAt, List<OrderItem> items,
                    List<OrderHistory> history) {
}
