package com.storemanager.domain.dashboard.model;

import java.math.BigDecimal;

public class DashboardPayrollSummary {

    private String employeeName;

    private BigDecimal totalHours;

    private BigDecimal totalSalary;

    public DashboardPayrollSummary() {
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public BigDecimal getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(BigDecimal totalHours) {
        this.totalHours = totalHours;
    }

    public BigDecimal getTotalSalary() {
        return totalSalary;
    }

    public void setTotalSalary(BigDecimal totalSalary) {
        this.totalSalary = totalSalary;
    }
}
