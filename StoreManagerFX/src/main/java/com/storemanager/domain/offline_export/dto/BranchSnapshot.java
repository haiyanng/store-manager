package com.storemanager.domain.offline_export.dto;

public record BranchSnapshot(
        Long sourceId,
        String name,
        String address,
        boolean active
) {
}
