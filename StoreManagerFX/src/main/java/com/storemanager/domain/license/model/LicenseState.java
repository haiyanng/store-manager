package com.storemanager.domain.license.model;

public enum LicenseState {

    ACTIVE,

    GRACE_PERIOD,

    EXPIRED,

    DEVICE_LIMIT_EXCEEDED;

    public boolean isLoginAllowed() {

        return this == ACTIVE || this == GRACE_PERIOD;
    }
}
