package com.storemanager.domain.dashboard.model;

import java.util.ArrayList;
import java.util.List;

public class EmployeeDashboardDto {

    private String title;

    private String subtitle;

    private List<DashboardMetricCard> metrics =
            new ArrayList<>();

    private String todayWorkedHours;

    private String currentMonthWorkedHours;

    private String assignedBranches;

    private List<DashboardAttendanceHistoryRow> recentAttendanceHistory =
            new ArrayList<>();

    public EmployeeDashboardDto() {
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

    public String getTodayWorkedHours() {
        return todayWorkedHours;
    }

    public void setTodayWorkedHours(String todayWorkedHours) {
        this.todayWorkedHours = todayWorkedHours;
    }

    public String getCurrentMonthWorkedHours() {
        return currentMonthWorkedHours;
    }

    public void setCurrentMonthWorkedHours(String currentMonthWorkedHours) {
        this.currentMonthWorkedHours = currentMonthWorkedHours;
    }

    public String getAssignedBranches() {
        return assignedBranches;
    }

    public void setAssignedBranches(String assignedBranches) {
        this.assignedBranches = assignedBranches;
    }

    public List<DashboardAttendanceHistoryRow> getRecentAttendanceHistory() {
        return recentAttendanceHistory;
    }

    public void setRecentAttendanceHistory(List<DashboardAttendanceHistoryRow> recentAttendanceHistory) {
        this.recentAttendanceHistory = recentAttendanceHistory;
    }
}
