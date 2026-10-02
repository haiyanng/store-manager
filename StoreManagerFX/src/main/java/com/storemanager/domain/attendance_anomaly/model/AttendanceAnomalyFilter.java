package com.storemanager.domain.attendance_anomaly.model;

import java.time.LocalDate;

public class AttendanceAnomalyFilter {

    private AttendanceAnomalyStatus status;

    private AttendanceAnomalyType type;

    private AttendanceAnomalySeverity severity;

    private Long employeeId;

    private LocalDate fromDate;

    private LocalDate toDate;

    public AttendanceAnomalyFilter() {
    }

    public AttendanceAnomalyStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceAnomalyStatus status) {
        this.status = status;
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

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }
}
