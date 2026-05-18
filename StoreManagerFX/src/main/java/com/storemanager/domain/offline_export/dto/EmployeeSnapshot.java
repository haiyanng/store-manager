package com.storemanager.domain.offline_export.dto;

public record EmployeeSnapshot(
        Long sourceId,
        String fullName,
        String phone,
        String address,
        String position,
        boolean active,
        Long sourceUserId,
        String imagePath
) {
}
