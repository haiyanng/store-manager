package com.storemanager.domain.attendance.model;

import java.math.BigDecimal;

public class AttendanceMonthlyTotal {

    private Long employeeId;

    private String month;

    private BigDecimal totalWorkedHours;

    public AttendanceMonthlyTotal() {
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(
            Long employeeId
    ) {
        this.employeeId = employeeId;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(
            String month
    ) {
        this.month = month;
    }

    public BigDecimal getTotalWorkedHours() {
        return totalWorkedHours;
    }

    public void setTotalWorkedHours(
            BigDecimal totalWorkedHours
    ) {
        this.totalWorkedHours = totalWorkedHours;
    }
}
