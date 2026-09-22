package com.storeapi.order.dto;

import java.time.LocalDateTime;

public record OrderHistoryDto(Long id, String action, String oldStatus, String newStatus,
                              String note, LocalDateTime createdAt) {
}
