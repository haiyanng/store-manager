package com.storemanager.domain.license.model;

import java.time.LocalDateTime;

public record LicenseDevice(

        String installationId,

        String deviceName,

        LocalDateTime activatedAt,

        LocalDateTime lastVerifiedAt,

        boolean active

) {
}
