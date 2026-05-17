package com.storemanager.domain.dashboard.model;

import com.storemanager.domain.user.model.RoleType;

import java.util.ArrayList;
import java.util.List;

public class DashboardAnalyticsSnapshot {

    private RoleType role;

    private String title;

    private String subtitle;

    private boolean showOwnSection;

    private boolean showManagerSection;

    private boolean showFinanceSection;

    private EmployeeDashboardDto employeeDashboard;

    private ManagerDashboardDto managerDashboard;

    private OwnerDashboardDto ownerDashboard;

    private List<DashboardMetricCard> metrics =
            new ArrayList<>();

    private String ownAttendanceSummary;

    private String ownPayrollSummary;

    private String ownBranchesSummary;

    private List<DashboardBranchSummary> branchSummaries =
            new ArrayList<>();

    private List<DashboardEmployeeLocationRow> employeeLocations =
            new ArrayList<>();

    private List<DashboardPayrollSummary> payrollSummaries =
            new ArrayList<>();

    private List<DashboardCashFlowSummary> cashFlowSummaries =
            new ArrayList<>();

    public DashboardAnalyticsSnapshot() {
    }

    public RoleType getRole() {
        return role;
    }

    public void setRole(RoleType role) {
        this.role = role;
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

    public boolean isShowOwnSection() {
        return showOwnSection;
    }

    public void setShowOwnSection(boolean showOwnSection) {
        this.showOwnSection = showOwnSection;
    }

    public boolean isShowManagerSection() {
        return showManagerSection;
    }

    public void setShowManagerSection(boolean showManagerSection) {
        this.showManagerSection = showManagerSection;
    }

    public boolean isShowFinanceSection() {
        return showFinanceSection;
    }

    public void setShowFinanceSection(boolean showFinanceSection) {
        this.showFinanceSection = showFinanceSection;
    }

    public EmployeeDashboardDto getEmployeeDashboard() {
        return employeeDashboard;
    }

    public void setEmployeeDashboard(EmployeeDashboardDto employeeDashboard) {
        this.employeeDashboard = employeeDashboard;
    }

    public ManagerDashboardDto getManagerDashboard() {
        return managerDashboard;
    }

    public void setManagerDashboard(ManagerDashboardDto managerDashboard) {
        this.managerDashboard = managerDashboard;
    }

    public OwnerDashboardDto getOwnerDashboard() {
        return ownerDashboard;
    }

    public void setOwnerDashboard(OwnerDashboardDto ownerDashboard) {
        this.ownerDashboard = ownerDashboard;
    }

    public List<DashboardMetricCard> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<DashboardMetricCard> metrics) {
        this.metrics = metrics;
    }

    public String getOwnAttendanceSummary() {
        return ownAttendanceSummary;
    }

    public void setOwnAttendanceSummary(String ownAttendanceSummary) {
        this.ownAttendanceSummary = ownAttendanceSummary;
    }

    public String getOwnPayrollSummary() {
        return ownPayrollSummary;
    }

    public void setOwnPayrollSummary(String ownPayrollSummary) {
        this.ownPayrollSummary = ownPayrollSummary;
    }

    public String getOwnBranchesSummary() {
        return ownBranchesSummary;
    }

    public void setOwnBranchesSummary(String ownBranchesSummary) {
        this.ownBranchesSummary = ownBranchesSummary;
    }

    public List<DashboardBranchSummary> getBranchSummaries() {
        return branchSummaries;
    }

    public void setBranchSummaries(List<DashboardBranchSummary> branchSummaries) {
        this.branchSummaries = branchSummaries;
    }

    public List<DashboardEmployeeLocationRow> getEmployeeLocations() {
        return employeeLocations;
    }

    public void setEmployeeLocations(List<DashboardEmployeeLocationRow> employeeLocations) {
        this.employeeLocations = employeeLocations;
    }

    public List<DashboardPayrollSummary> getPayrollSummaries() {
        return payrollSummaries;
    }

    public void setPayrollSummaries(List<DashboardPayrollSummary> payrollSummaries) {
        this.payrollSummaries = payrollSummaries;
    }

    public List<DashboardCashFlowSummary> getCashFlowSummaries() {
        return cashFlowSummaries;
    }

    public void setCashFlowSummaries(List<DashboardCashFlowSummary> cashFlowSummaries) {
        this.cashFlowSummaries = cashFlowSummaries;
    }
}
