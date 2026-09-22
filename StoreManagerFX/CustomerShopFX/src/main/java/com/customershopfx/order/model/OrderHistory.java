package com.customershopfx.order.model;

import java.time.LocalDateTime;

public record OrderHistory(Long id, String action, String oldStatus, String newStatus,
                           String note, LocalDateTime createdAt) {
}
