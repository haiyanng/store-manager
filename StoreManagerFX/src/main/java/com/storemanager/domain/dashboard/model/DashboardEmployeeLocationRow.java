package com.storemanager.domain.dashboard.model;

public class DashboardEmployeeLocationRow {

    private String employeeName;

    private String branches;

    private long activeAssignments;

    public DashboardEmployeeLocationRow() {
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getBranches() {
        return branches;
    }

    public void setBranches(String branches) {
        this.branches = branches;
    }

    public long getActiveAssignments() {
        return activeAssignments;
    }

    public void setActiveAssignments(long activeAssignments) {
        this.activeAssignments = activeAssignments;
    }
}
