package com.storemanager.domain.offline_export.dto;

public record CategorySnapshot(
        Long sourceId,
        String name,
        boolean active,
        String imagePath
) {
}
