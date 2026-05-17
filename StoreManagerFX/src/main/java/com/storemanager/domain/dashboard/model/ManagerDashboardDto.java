package com.storemanager.domain.dashboard.model;

import java.util.ArrayList;
import java.util.List;

public class ManagerDashboardDto {

    private String title;

    private String subtitle;

    private List<DashboardMetricCard> metrics =
            new ArrayList<>();

    private long branchEmployeeCount;

    private long employeesCurrentlyCheckedIn;

    private String attendanceOverview;

    private String inventoryOverview;

    private String salesOverview;

    private List<DashboardBranchSummary> branchOperationalSummaries =
            new ArrayList<>();

    private List<DashboardEmployeeLocationRow> employeeLocations =
            new ArrayList<>();

    public ManagerDashboardDto() {
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

    public long getBranchEmployeeCount() {
        return branchEmployeeCount;
    }

    public void setBranchEmployeeCount(long branchEmployeeCount) {
        this.branchEmployeeCount = branchEmployeeCount;
    }

    public long getEmployeesCurrentlyCheckedIn() {
        return employeesCurrentlyCheckedIn;
    }

    public void setEmployeesCurrentlyCheckedIn(long employeesCurrentlyCheckedIn) {
        this.employeesCurrentlyCheckedIn = employeesCurrentlyCheckedIn;
    }

    public String getAttendanceOverview() {
        return attendanceOverview;
    }

    public void setAttendanceOverview(String attendanceOverview) {
        this.attendanceOverview = attendanceOverview;
    }

    public String getInventoryOverview() {
        return inventoryOverview;
    }

    public void setInventoryOverview(String inventoryOverview) {
        this.inventoryOverview = inventoryOverview;
    }

    public String getSalesOverview() {
        return salesOverview;
    }

    public void setSalesOverview(String salesOverview) {
        this.salesOverview = salesOverview;
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
}
