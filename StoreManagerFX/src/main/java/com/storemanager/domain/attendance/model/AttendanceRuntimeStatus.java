package com.storemanager.domain.attendance.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AttendanceRuntimeStatus {

    private AttendanceState state;

    private AttendanceSession activeSession;

    private String activeBranchName;

    private BigDecimal todayWorkedHours;

    private LocalDateTime latestCheckInTime;

    private String statusLabel;

    public AttendanceState getState() {
        return state;
    }

    public void setState(AttendanceState state) {
        this.state = state;
    }

    public AttendanceSession getActiveSession() {
        return activeSession;
    }

    public void setActiveSession(AttendanceSession activeSession) {
        this.activeSession = activeSession;
    }

    public String getActiveBranchName() {
        return activeBranchName;
    }

    public void setActiveBranchName(String activeBranchName) {
        this.activeBranchName = activeBranchName;
    }

    public BigDecimal getTodayWorkedHours() {
        return todayWorkedHours;
    }

    public void setTodayWorkedHours(BigDecimal todayWorkedHours) {
        this.todayWorkedHours = todayWorkedHours;
    }

    public LocalDateTime getLatestCheckInTime() {
        return latestCheckInTime;
    }

    public void setLatestCheckInTime(LocalDateTime latestCheckInTime) {
        this.latestCheckInTime = latestCheckInTime;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
    }
}
