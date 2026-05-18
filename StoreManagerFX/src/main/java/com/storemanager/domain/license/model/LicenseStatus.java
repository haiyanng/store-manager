package com.storemanager.domain.license.model;

import java.time.LocalDateTime;
import java.util.List;

public record LicenseStatus(

        LicenseState state,

        String message,

        boolean loginAllowed,

        int allowedSlots,

        List<LicenseDevice> activatedDevices,

        String currentInstallationId,

        LocalDateTime nextVerificationAt,

        LocalDateTime graceUntil,

        LocalDateTime lastVerifiedAt

) {

    public LicenseStatus {

        activatedDevices =
                activatedDevices == null
                        ? List.of()
                        : List.copyOf(activatedDevices);
    }

    public boolean isGracePeriod() {

        return state == LicenseState.GRACE_PERIOD;
    }

    public boolean isBlocked() {

        return !loginAllowed;
    }
}
