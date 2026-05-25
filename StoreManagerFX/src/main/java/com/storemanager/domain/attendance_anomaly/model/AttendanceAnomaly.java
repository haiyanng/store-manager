package com.storemanager.domain.attendance_anomaly.model;

import java.time.LocalDateTime;

public class AttendanceAnomaly {

    private Long id;

    private Long attendanceSessionId;

    private Long employeeId;

    private Long branchId;

    private AttendanceAnomalyType type;

    private AttendanceAnomalySeverity severity;

    private String message;

    private AttendanceAnomalyStatus status;

    private boolean employeeNotified;

    private boolean managerReported;

    private String managerReportText;

    private Long resolvedByUserId;

    private LocalDateTime resolvedAt;

    private LocalDateTime createdAt;

    public AttendanceAnomaly() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAttendanceSessionId() {
        return attendanceSessionId;
    }

    public void setAttendanceSessionId(Long attendanceSessionId) {
        this.attendanceSessionId = attendanceSessionId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public Long getBranchId() {
        return branchId;
    }

    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }

    public AttendanceAnomalyType getType() {
        return type;
    }

    public void setType(AttendanceAnomalyType type) {
        this.type = type;
    }

    public AttendanceAnomalySeverity getSeverity() {
        return severity;
    }

    public void setSeverity(AttendanceAnomalySeverity severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public AttendanceAnomalyStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceAnomalyStatus status) {
        this.status = status;
    }

    public boolean isEmployeeNotified() {
        return employeeNotified;
    }

    public void setEmployeeNotified(boolean employeeNotified) {
        this.employeeNotified = employeeNotified;
    }

    public boolean isManagerReported() {
        return managerReported;
    }

    public void setManagerReported(boolean managerReported) {
        this.managerReported = managerReported;
    }

    public String getManagerReportText() {
        return managerReportText;
    }

    public void setManagerReportText(String managerReportText) {
        this.managerReportText = managerReportText;
    }

    public Long getResolvedByUserId() {
        return resolvedByUserId;
    }

    public void setResolvedByUserId(Long resolvedByUserId) {
        this.resolvedByUserId = resolvedByUserId;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
