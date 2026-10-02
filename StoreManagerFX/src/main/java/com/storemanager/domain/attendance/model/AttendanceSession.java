package com.storemanager.domain.attendance.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AttendanceSession {

    private Long id;

    private Long employeeId;

    private LocalDateTime checkInTime;

    private LocalDateTime checkOutTime;

    private BigDecimal workedHours;

    private Long createdByUserId;

    private LocalDateTime createdAt;

    public AttendanceSession() {
    }

    public Long getId() {
        return id;
    }

    public void setId(
            Long id
    ) {
        this.id = id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(
            Long employeeId
    ) {
        this.employeeId = employeeId;
    }

    public LocalDateTime getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(
            LocalDateTime checkInTime
    ) {
        this.checkInTime = checkInTime;
    }

    public LocalDateTime getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(
            LocalDateTime checkOutTime
    ) {
        this.checkOutTime = checkOutTime;
    }

    public BigDecimal getWorkedHours() {
        return workedHours;
    }

    public void setWorkedHours(
            BigDecimal workedHours
    ) {
        this.workedHours = workedHours;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(
            Long createdByUserId
    ) {
        this.createdByUserId = createdByUserId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(
            LocalDateTime createdAt
    ) {
        this.createdAt = createdAt;
    }
}
