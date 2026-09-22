package com.storeapi.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDto(Long id, String status, BigDecimal totalAmount, String recipientName,
                       String phone, String shippingAddress, String paymentMethod,
                       LocalDateTime createdAt, List<OrderItemDto> items,
                       List<OrderHistoryDto> history) {
}
