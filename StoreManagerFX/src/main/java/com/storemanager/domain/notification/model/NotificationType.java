package com.storemanager.domain.notification.model;

public enum NotificationType {
    // Persistence compatibility for historical notifications only.
    @Deprecated
    PAYROLL,
    INVENTORY,
    ATTENDANCE,
    ATTENDANCE_ANOMALY,
    SYSTEM;

    public static NotificationType fromStoredValue(String value) {
        if (value == null) return SYSTEM;
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            // Retired notification categories remain readable as system history.
            return SYSTEM;
        }
    }
}
