package com.storemanager.domain.notification.model;

public enum NotificationType {
    // Persistence compatibility for historical notifications only.
    @Deprecated
    PAYROLL,
    INVENTORY,
    ATTENDANCE,
    ATTENDANCE_ANOMALY,
    SYSTEM,
    BRANCH
}
