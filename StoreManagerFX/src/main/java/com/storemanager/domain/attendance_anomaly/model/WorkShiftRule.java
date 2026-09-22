package com.storemanager.domain.attendance_anomaly.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class WorkShiftRule {

    private Long id;

    private Long branchId;

    private String shiftName;

    private LocalTime startTime;

    private LocalTime endTime;

    private Integer lateToleranceMinutes;

    private Integer earlyToleranceMinutes;

    private BigDecimal maxWorkHours;

    private boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public WorkShiftRule() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBranchId() {
        return branchId;
    }

    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }

    public String getShiftName() {
        return shiftName;
    }

    public void setShiftName(String shiftName) {
        this.shiftName = shiftName;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public Integer getLateToleranceMinutes() {
        return lateToleranceMinutes;
    }

    public void setLateToleranceMinutes(Integer lateToleranceMinutes) {
        this.lateToleranceMinutes = lateToleranceMinutes;
    }

    public Integer getEarlyToleranceMinutes() {
        return earlyToleranceMinutes;
    }

    public void setEarlyToleranceMinutes(Integer earlyToleranceMinutes) {
        this.earlyToleranceMinutes = earlyToleranceMinutes;
    }

    public BigDecimal getMaxWorkHours() {
        return maxWorkHours;
    }

    public void setMaxWorkHours(BigDecimal maxWorkHours) {
        this.maxWorkHours = maxWorkHours;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
