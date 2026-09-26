package com.storemanager.domain.dashboard.model;

import java.util.ArrayList;
import java.util.List;

public class OwnerDashboardDto {

    private String title;

    private String subtitle;

    private List<DashboardMetricCard> metrics =
            new ArrayList<>();

    private long totalEmployees;

    private long activeEmployees;

    private long employeesCurrentlyWorking;

    private String revenueTotals;

    private String inventoryAlertsSummary;

    private List<DashboardBranchSummary> branchOperationalSummaries =
            new ArrayList<>();

    private List<DashboardEmployeeLocationRow> employeeLocations =
            new ArrayList<>();

    private List<DashboardCashFlowSummary> cashFlowSummaries =
            new ArrayList<>();

    private List<DashboardInventoryAlertRow> inventoryAlerts =
            new ArrayList<>();

    public OwnerDashboardDto() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public List<DashboardMetricCard> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<DashboardMetricCard> metrics) {
        this.metrics = metrics;
    }

    public long getTotalEmployees() {
        return totalEmployees;
    }

    public void setTotalEmployees(long totalEmployees) {
        this.totalEmployees = totalEmployees;
    }

    public long getActiveEmployees() {
        return activeEmployees;
    }

    public void setActiveEmployees(long activeEmployees) {
        this.activeEmployees = activeEmployees;
    }

    public long getEmployeesCurrentlyWorking() {
        return employeesCurrentlyWorking;
    }

    public void setEmployeesCurrentlyWorking(long employeesCurrentlyWorking) {
        this.employeesCurrentlyWorking = employeesCurrentlyWorking;
    }

    public String getRevenueTotals() {
        return revenueTotals;
    }

    public void setRevenueTotals(String revenueTotals) {
        this.revenueTotals = revenueTotals;
    }

    public String getInventoryAlertsSummary() {
        return inventoryAlertsSummary;
    }

    public void setInventoryAlertsSummary(String inventoryAlertsSummary) {
        this.inventoryAlertsSummary = inventoryAlertsSummary;
    }



    public List<DashboardBranchSummary> getBranchOperationalSummaries() {
        return branchOperationalSummaries;
    }

    public void setBranchOperationalSummaries(List<DashboardBranchSummary> branchOperationalSummaries) {
        this.branchOperationalSummaries = branchOperationalSummaries;
    }

    public List<DashboardEmployeeLocationRow> getEmployeeLocations() {
        return employeeLocations;
    }

    public void setEmployeeLocations(List<DashboardEmployeeLocationRow> employeeLocations) {
        this.employeeLocations = employeeLocations;
    }

    public List<DashboardCashFlowSummary> getCashFlowSummaries() {
        return cashFlowSummaries;
    }

    public void setCashFlowSummaries(List<DashboardCashFlowSummary> cashFlowSummaries) {
        this.cashFlowSummaries = cashFlowSummaries;
    }

    public List<DashboardInventoryAlertRow> getInventoryAlerts() {
        return inventoryAlerts;
    }

    public void setInventoryAlerts(List<DashboardInventoryAlertRow> inventoryAlerts) {
        this.inventoryAlerts = inventoryAlerts;
    }
}
