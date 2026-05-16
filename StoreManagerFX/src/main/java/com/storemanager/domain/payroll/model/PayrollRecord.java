package com.storemanager.domain.payroll.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PayrollRecord {

    private Long id;

    private Long employeeId;

    private int month;

    private int year;

    private BigDecimal totalHours;

    private BigDecimal hourlyRateSnapshot;

    private BigDecimal totalSalary;

    private LocalDateTime generatedAt;

    private Long generatedByUserId;

    public PayrollRecord() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public BigDecimal getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(BigDecimal totalHours) {
        this.totalHours = totalHours;
    }

    public BigDecimal getHourlyRateSnapshot() {
        return hourlyRateSnapshot;
    }

    public void setHourlyRateSnapshot(BigDecimal hourlyRateSnapshot) {
        this.hourlyRateSnapshot = hourlyRateSnapshot;
    }

    public BigDecimal getTotalSalary() {
        return totalSalary;
    }

    public void setTotalSalary(BigDecimal totalSalary) {
        this.totalSalary = totalSalary;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public Long getGeneratedByUserId() {
        return generatedByUserId;
    }

    public void setGeneratedByUserId(Long generatedByUserId) {
        this.generatedByUserId = generatedByUserId;
    }
}
