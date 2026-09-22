package com.storemanager.domain.attendance_anomaly.model;

public enum AttendanceAnomalyType {
    CHECK_IN_TOO_EARLY,
    CHECK_IN_TOO_LATE,
    CHECK_OUT_TOO_EARLY,
    CHECK_OUT_TOO_LATE,
    WORKED_TOO_SHORT,
    WORKED_TOO_LONG,
    MISSING_CHECK_OUT,
    OUT_OF_SHIFT
}
