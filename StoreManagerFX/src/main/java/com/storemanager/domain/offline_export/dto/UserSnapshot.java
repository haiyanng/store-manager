package com.storemanager.domain.offline_export.dto;

import com.storemanager.domain.user.model.RoleType;

import java.time.LocalDateTime;

public record UserSnapshot(
        Long sourceId,
        String username,
        RoleType role,
        boolean active,
        LocalDateTime createdAt
) {
}
