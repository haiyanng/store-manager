package com.storemanager.domain.dashboard.model;

import java.math.BigDecimal;

public class DashboardBranchSummary {

    private String branchName;

    private long attendanceSessions;

    private BigDecimal attendanceHours;

    private long assignedEmployees;

    public DashboardBranchSummary() {
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public long getAttendanceSessions() {
        return attendanceSessions;
    }

    public void setAttendanceSessions(long attendanceSessions) {
        this.attendanceSessions = attendanceSessions;
    }

    public BigDecimal getAttendanceHours() {
        return attendanceHours;
    }

    public void setAttendanceHours(BigDecimal attendanceHours) {
        this.attendanceHours = attendanceHours;
    }

    public long getAssignedEmployees() {
        return assignedEmployees;
    }

    public void setAssignedEmployees(long assignedEmployees) {
        this.assignedEmployees = assignedEmployees;
    }
}
