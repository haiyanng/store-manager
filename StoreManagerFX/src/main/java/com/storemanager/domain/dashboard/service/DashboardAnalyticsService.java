package com.storemanager.domain.dashboard.service;

import com.storemanager.core.security.PermissionGuard;
import com.storemanager.core.session.AppSession;
import com.storemanager.domain.attendance.model.AttendanceSession;
import com.storemanager.domain.attendance.service.AttendanceService;
import com.storemanager.domain.branch.model.Branch;
import com.storemanager.domain.branch.model.EmployeeBranchAssignment;
import com.storemanager.domain.branch.service.BranchService;
import com.storemanager.domain.dashboard.model.DashboardAnalyticsSnapshot;
import com.storemanager.domain.dashboard.model.DashboardAttendanceHistoryRow;
import com.storemanager.domain.dashboard.model.DashboardBranchSummary;
import com.storemanager.domain.dashboard.model.DashboardCashFlowSummary;
import com.storemanager.domain.dashboard.model.DashboardEmployeeLocationRow;
import com.storemanager.domain.dashboard.model.DashboardInventoryAlertRow;
import com.storemanager.domain.dashboard.model.DashboardMetricCard;
import com.storemanager.domain.dashboard.model.DashboardPayrollSummary;
import com.storemanager.domain.dashboard.model.EmployeeDashboardDto;
import com.storemanager.domain.dashboard.model.ManagerDashboardDto;
import com.storemanager.domain.dashboard.model.OwnerDashboardDto;
import com.storemanager.domain.employee.model.Employee;
import com.storemanager.domain.employee.service.EmployeeService;
import com.storemanager.domain.importing.service.ImportService;
import com.storemanager.domain.inventory.service.InventoryService;
import com.storemanager.domain.payroll.model.PayrollRecord;
import com.storemanager.domain.payroll.service.PayrollService;
import com.storemanager.domain.sale.service.SaleService;
import com.storemanager.domain.user.model.RoleType;
import com.storemanager.domain.user.model.User;
import com.storemanager.core.util.TimeFormatUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DashboardAnalyticsService {

    private final EmployeeService employeeService =
            new EmployeeService();

    private final AttendanceService attendanceService =
            new AttendanceService();

    private final BranchService branchService =
            new BranchService();

    private final PayrollService payrollService =
            new PayrollService();

    private final SaleService saleService =
            new SaleService();

    private final ImportService importService =
            new ImportService();

    private final InventoryService inventoryService =
            new InventoryService();

    public DashboardAnalyticsSnapshot loadDashboardAnalytics() {

        User currentUser =
                AppSession.getCurrentUser();

        if (currentUser == null) {
            throw new RuntimeException("User session is required");
        }

        if (PermissionGuard.isEmployee()) {
            return buildEmployeeSnapshot(currentUser);
        }

        if (PermissionGuard.isManager()) {
            return buildManagerSnapshot(currentUser);
        }

        return buildExecutiveSnapshot(currentUser);
    }

    private DashboardAnalyticsSnapshot buildEmployeeSnapshot(
            User currentUser
    ) {

        Employee employee =
                employeeService.findByUserId(currentUser.getId());

        EmployeeDashboardDto employeeDashboard =
                buildEmployeeDashboard(employee);

        DashboardAnalyticsSnapshot snapshot =
                createBaseSnapshot(
                        currentUser.getRole(),
                        employeeDashboard.getTitle(),
                        employeeDashboard.getSubtitle()
                );

        snapshot.setShowOwnSection(true);
        snapshot.setEmployeeDashboard(employeeDashboard);
        snapshot.setMetrics(employeeDashboard.getMetrics());

        return snapshot;
    }

    private DashboardAnalyticsSnapshot buildManagerSnapshot(
            User currentUser
    ) {

        ManagerDashboardDto managerDashboard =
                buildManagerDashboard();

        DashboardAnalyticsSnapshot snapshot =
                createBaseSnapshot(
                        currentUser.getRole(),
                        managerDashboard.getTitle(),
                        managerDashboard.getSubtitle()
                );

        snapshot.setShowManagerSection(true);
        snapshot.setManagerDashboard(managerDashboard);
        snapshot.setMetrics(managerDashboard.getMetrics());

        return snapshot;
    }

    private DashboardAnalyticsSnapshot buildExecutiveSnapshot(
            User currentUser
    ) {

        OwnerDashboardDto ownerDashboard =
                buildOwnerDashboard();

        DashboardAnalyticsSnapshot snapshot =
                createBaseSnapshot(
                        currentUser.getRole(),
                        ownerDashboard.getTitle(),
                        ownerDashboard.getSubtitle()
                );

        snapshot.setShowManagerSection(true);
        snapshot.setShowFinanceSection(true);
        snapshot.setOwnerDashboard(ownerDashboard);
        snapshot.setMetrics(ownerDashboard.getMetrics());

        return snapshot;
    }

    private DashboardAnalyticsSnapshot createBaseSnapshot(
            RoleType role,
            String title,
            String subtitle
    ) {

        DashboardAnalyticsSnapshot snapshot =
                new DashboardAnalyticsSnapshot();

        snapshot.setRole(role);
        snapshot.setTitle(title);
        snapshot.setSubtitle(subtitle);

        return snapshot;
    }

    private EmployeeDashboardDto buildEmployeeDashboard(
            Employee employee
    ) {

        EmployeeDashboardDto dto =
                new EmployeeDashboardDto();

        dto.setTitle("My Work");
        dto.setSubtitle("Your attendance, payroll, and assigned branches");
        dto.setMetrics(buildEmployeeMetrics(employee));
        dto.setTodayWorkedHours(
                formatHours(sumAttendanceHoursForEmployeeToday(employee))
        );
        dto.setCurrentMonthWorkedHours(
                formatHours(sumAttendanceHoursForEmployeeCurrentMonth(employee))
        );
        dto.setAssignedBranches(buildOwnBranchesSummary(employee));
        dto.setLatestPayrollSummary(buildLatestPayrollSummary(employee));
        dto.setRecentAttendanceHistory(
                buildRecentAttendanceHistory(employee, 5)
        );

        return dto;
    }

    private ManagerDashboardDto buildManagerDashboard() {

        ManagerDashboardDto dto =
                new ManagerDashboardDto();

        dto.setTitle("Operational Overview");
        dto.setSubtitle(
                "Branch attendance, employee locations, inventory, and sales activity"
        );
        dto.setMetrics(buildManagerMetrics());
        dto.setBranchEmployeeCount(countUniqueEmployeesAssignedToBranches());
        dto.setEmployeesCurrentlyCheckedIn(countEmployeesCurrentlyCheckedIn());
        dto.setAttendanceOverview(buildAttendanceOverviewSummary());
        dto.setInventoryOverview(buildInventoryOverviewSummary());
        dto.setSalesOverview(buildSalesOverviewSummary());
        dto.setBranchOperationalSummaries(buildBranchSummaries());
        dto.setEmployeeLocations(buildEmployeeLocationRows());

        return dto;
    }

    private OwnerDashboardDto buildOwnerDashboard() {

        OwnerDashboardDto dto =
                new OwnerDashboardDto();

        dto.setTitle("Business Control");
        dto.setSubtitle(
                "Full business analytics, payroll totals, revenue totals, and cash flow"
        );
        dto.setMetrics(buildOwnerMetrics());
        dto.setTotalEmployees(countTotalEmployees());
        dto.setActiveEmployees(countActiveEmployees());
        dto.setEmployeesCurrentlyWorking(countEmployeesCurrentlyCheckedIn());
        dto.setPayrollTotals(buildPayrollTotalsSummary());
        dto.setRevenueTotals(buildRevenueTotalsSummary());
        dto.setInventoryAlertsSummary(buildInventoryAlertsSummary());
        dto.setBranchOperationalSummaries(buildBranchSummaries());
        dto.setEmployeeLocations(buildEmployeeLocationRows());
        dto.setPayrollSummaries(buildPayrollSummaries());
        dto.setCashFlowSummaries(buildCashFlowSummaries());
        dto.setInventoryAlerts(buildInventoryAlertRows());

        return dto;
    }

    private List<DashboardMetricCard> buildEmployeeMetrics(
            Employee employee
    ) {

        BigDecimal attendanceHours =
                sumAttendanceHoursForEmployeeCurrentMonth(employee);

        long branchCount =
                countAssignedBranchesForEmployee(employee);

        return List.of(
                new DashboardMetricCard(
                        "Today's hours",
                        formatDurationHours(
                                sumAttendanceHoursForEmployeeToday(employee)
                        ),
                        "Worked today in assigned branches"
                ),
                new DashboardMetricCard(
                        "Month hours",
                        formatDurationHours(attendanceHours),
                        "Current month worked hours"
                ),
                new DashboardMetricCard(
                        "Assigned branches",
                        String.valueOf(branchCount),
                        "Active branch assignments"
                )
        );
    }

    private List<DashboardMetricCard> buildManagerMetrics() {

        BigDecimal attendanceHours =
                sumAttendanceHoursForCurrentMonthByBranch();

        BigDecimal inventoryValue =
                inventoryService.findInventoryValue();

        long orderCount =
                saleService.countOrdersForCurrentMonth();

        long branchCount =
                branchService.findBranches()
                        .stream()
                        .filter(Branch::isActive)
                        .count();

        long checkedIn =
                countEmployeesCurrentlyCheckedIn();

        return List.of(
                new DashboardMetricCard(
                        "Checked in",
                        String.valueOf(checkedIn),
                        "Employees currently on shift"
                ),
                new DashboardMetricCard(
                        "Branch attendance",
                        formatDurationHours(attendanceHours),
                        "Current month across all branches"
                ),
                new DashboardMetricCard(
                        "Inventory value",
                        formatMoney(inventoryValue),
                        "Business inventory snapshot"
                ),
                new DashboardMetricCard(
                        "Sales orders",
                        String.valueOf(orderCount),
                        "Current month order count"
                ),
                new DashboardMetricCard(
                        "Active branches",
                        String.valueOf(branchCount),
                        "Branch structure in operation"
                )
        );
    }

    private List<DashboardMetricCard> buildExecutiveMetrics() {

        return buildOwnerMetrics();
    }

    private List<DashboardMetricCard> buildOwnerMetrics() {

        BigDecimal revenueMonth =
                saleService.findRevenueForCurrentMonth();

        BigDecimal revenueAllTime =
                saleService.findTotalRevenue();

        BigDecimal importAllTime =
                importService.findTotalImportCost();

        BigDecimal payrollAllTime =
                payrollService.findTotalPayrollCost();

        long totalEmployees =
                countTotalEmployees();

        long activeEmployees =
                countActiveEmployees();

        long workingEmployees =
                countEmployeesCurrentlyCheckedIn();

        long inventoryAlerts =
                buildInventoryAlertRows().size();

        return List.of(
                new DashboardMetricCard(
                        "Total employees",
                        String.valueOf(totalEmployees),
                        "All employee records"
                ),
                new DashboardMetricCard(
                        "Active employees",
                        String.valueOf(activeEmployees),
                        "Currently active staff"
                ),
                new DashboardMetricCard(
                        "Working now",
                        String.valueOf(workingEmployees),
                        "Open attendance sessions"
                ),
                new DashboardMetricCard(
                        "Revenue total",
                        formatMoney(revenueAllTime),
                        "All-time sales revenue"
                ),
                new DashboardMetricCard(
                        "Payroll total",
                        formatMoney(payrollAllTime),
                        "All-time HR cash flow"
                ),
                new DashboardMetricCard(
                        "Inventory alerts",
                        String.valueOf(inventoryAlerts),
                        "Low stock items requiring attention"
                )
        );
    }

    private String buildOwnAttendanceSummary(
            Employee employee
    ) {

        if (employee == null) {
            return "No employee profile linked to the current account";
        }

        BigDecimal hours =
                sumAttendanceHoursForEmployeeCurrentMonth(employee);

        long sessions =
                countAttendanceSessionsForEmployeeCurrentMonth(employee);

        return "Current month: "
                + formatDurationHours(hours)
                + " across "
                + sessions
                + " sessions";
    }

    private String buildOwnPayrollSummary(
            Employee employee
    ) {

        if (employee == null) {
            return "No payroll profile available";
        }

        return buildLatestPayrollSummary(employee);
    }

    private String buildOwnBranchesSummary(
            Employee employee
    ) {

        if (employee == null) {
            return "No assigned branches";
        }

        List<Branch> assignedBranches =
                branchService.findActiveBranchesForEmployeeId(
                        employee.getId()
                );

        String branchNames =
                assignedBranches
                        .stream()
                        .filter(branch -> branch != null)
                        .filter(Branch::isActive)
                        .map(Branch::getName)
                        .distinct()
                        .collect(Collectors.joining(", "));

        if (branchNames.isEmpty()) {
            return "No active branch assignments";
        }

        return branchNames;
    }

    private List<DashboardBranchSummary> buildBranchSummaries() {

        Map<Long, Branch> branchesById =
                branchService.findBranchesById();

        Map<Long, Long> activeAssignmentCounts =
                branchService.findAssignments()
                        .stream()
                        .filter(EmployeeBranchAssignment::isActive)
                        .collect(
                                Collectors.groupingBy(
                                        EmployeeBranchAssignment::getBranchId,
                                        Collectors.counting()
                                )
                        );

        Map<Long, List<AttendanceSession>> sessionsByBranch =
                attendanceService.findAllSessions()
                        .stream()
                        .filter(session -> session.getBranchId() != null)
                        .filter(this::isCurrentMonthSession)
                .collect(
                        Collectors.groupingBy(
                                AttendanceSession::getBranchId
                        )
                );

        return branchesById.values()
                .stream()
                .filter(Branch::isActive)
                .map(branch -> {
                    List<AttendanceSession> sessions =
                            sessionsByBranch.getOrDefault(
                                    branch.getId(),
                                    List.of()
                            );

                    DashboardBranchSummary summary =
                            new DashboardBranchSummary();

                    summary.setBranchName(branch.getName());
                    summary.setAttendanceSessions(
                            sessions.size()
                    );
                    summary.setAttendanceHours(
                            sessions
                                    .stream()
                                    .filter(session -> session.getWorkedHours() != null)
                                    .map(AttendanceSession::getWorkedHours)
                                    .reduce(
                                            BigDecimal.ZERO,
                                            BigDecimal::add
                                    )
                    );
                    summary.setAssignedEmployees(
                            activeAssignmentCounts.getOrDefault(
                                    branch.getId(),
                                    0L
                            )
                    );

                    return summary;
                })
                .sorted(
                        Comparator.comparing(
                                DashboardBranchSummary::getBranchName,
                                Comparator.nullsLast(String::compareToIgnoreCase)
                        )
                )
                .toList();
    }

    private List<DashboardEmployeeLocationRow> buildEmployeeLocationRows() {

        Map<Long, Employee> employeesById =
                branchService.findEmployeesById();

        Map<Long, Branch> branchesById =
                branchService.findBranchesById();

        return branchService.findAssignments()
                .stream()
                .filter(EmployeeBranchAssignment::isActive)
                .collect(
                        Collectors.groupingBy(
                                EmployeeBranchAssignment::getEmployeeId
                        )
                )
                .entrySet()
                .stream()
                .map(entry -> {
                    Employee employee =
                            employeesById.get(entry.getKey());

                    String branches =
                            entry.getValue()
                                    .stream()
                                    .map(EmployeeBranchAssignment::getBranchId)
                                    .map(branchesById::get)
                                    .filter(branch -> branch != null)
                                    .map(Branch::getName)
                                    .distinct()
                                    .collect(Collectors.joining(", "));

                    DashboardEmployeeLocationRow row =
                            new DashboardEmployeeLocationRow();

                    row.setEmployeeName(
                            employee == null
                                    ? "Unknown employee"
                                    : employee.getFullName()
                    );
                    row.setBranches(branches);
                    row.setActiveAssignments(
                            entry.getValue().size()
                    );

                    return row;
                })
                .sorted(
                        Comparator.comparing(
                                DashboardEmployeeLocationRow::getEmployeeName,
                                Comparator.nullsLast(String::compareToIgnoreCase)
                        )
                )
                .toList();
    }

    private List<DashboardPayrollSummary> buildPayrollSummaries() {

        Map<Long, Employee> employeesById =
                employeeService.findAll()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        Employee::getId,
                                        Function.identity()
                                )
                        );

        return payrollService.findPayrollRecords()
                .stream()
                .collect(
                        Collectors.groupingBy(
                                PayrollRecord::getEmployeeId
                        )
                )
                .entrySet()
                .stream()
                .map(entry -> {
                    DashboardPayrollSummary summary =
                            new DashboardPayrollSummary();

                    Employee employee =
                            employeesById.get(entry.getKey());

                    summary.setEmployeeName(
                            employee == null
                                    ? "Unknown employee"
                                    : employee.getFullName()
                    );
                    summary.setTotalHours(
                            entry.getValue()
                                    .stream()
                                    .map(PayrollRecord::getTotalHours)
                                    .reduce(
                                            BigDecimal.ZERO,
                                            BigDecimal::add
                                    )
                    );
                    summary.setTotalSalary(
                            entry.getValue()
                                    .stream()
                                    .map(PayrollRecord::getTotalSalary)
                                    .reduce(
                                            BigDecimal.ZERO,
                                            BigDecimal::add
                                    )
                    );

                    return summary;
                })
                .sorted(
                        Comparator.comparing(
                                DashboardPayrollSummary::getEmployeeName,
                                Comparator.nullsLast(String::compareToIgnoreCase)
                        )
                )
                .toList();
    }

    private List<DashboardCashFlowSummary> buildCashFlowSummaries() {

        BigDecimal revenueMonth =
                saleService.findRevenueForCurrentMonth();

        BigDecimal revenueAllTime =
                saleService.findTotalRevenue();

        BigDecimal importMonth =
                importService.findImportCostForCurrentMonth();

        BigDecimal importAllTime =
                importService.findTotalImportCost();

        BigDecimal payrollMonth =
                payrollService.findPayrollCostForCurrentMonth();

        BigDecimal payrollAllTime =
                payrollService.findTotalPayrollCost();

        DashboardCashFlowSummary monthSummary =
                new DashboardCashFlowSummary();
        monthSummary.setLabel("Current month");
        monthSummary.setRevenueTotal(revenueMonth);
        monthSummary.setImportCost(importMonth);
        monthSummary.setProductBusinessCashFlow(
                revenueMonth.subtract(importMonth)
        );
        monthSummary.setPayrollCashFlow(payrollMonth);
        monthSummary.setFinalCashFlow(
                revenueMonth.subtract(importMonth).subtract(payrollMonth)
        );

        DashboardCashFlowSummary totalSummary =
                new DashboardCashFlowSummary();
        totalSummary.setLabel("All time");
        totalSummary.setRevenueTotal(revenueAllTime);
        totalSummary.setImportCost(importAllTime);
        totalSummary.setProductBusinessCashFlow(
                revenueAllTime.subtract(importAllTime)
        );
        totalSummary.setPayrollCashFlow(payrollAllTime);
        totalSummary.setFinalCashFlow(
                revenueAllTime.subtract(importAllTime).subtract(payrollAllTime)
        );

        return List.of(monthSummary, totalSummary);
    }

    private BigDecimal sumAttendanceHoursForEmployee(
            Employee employee
    ) {

        if (employee == null) {
            return BigDecimal.ZERO;
        }

        return attendanceService.findSessionsByEmployeeId(
                        employee.getId()
                )
                .stream()
                .filter(this::isCurrentMonthSession)
                .filter(session -> session.getWorkedHours() != null)
                .map(AttendanceSession::getWorkedHours)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }

    private long countAttendanceSessionsForEmployee(
            Employee employee
    ) {

        if (employee == null) {
            return 0L;
        }

        return attendanceService.findSessionsByEmployeeId(
                        employee.getId()
                )
                .stream()
                .filter(this::isCurrentMonthSession)
                .count();
    }

    private BigDecimal sumPayrollForEmployee(
            Employee employee
    ) {

        if (employee == null) {
            return BigDecimal.ZERO;
        }

        return payrollService.findPayrollRecordsByEmployeeId(
                        employee.getId()
                )
                .stream()
                .filter(this::isCurrentMonthPayrollRecord)
                .map(PayrollRecord::getTotalSalary)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }

    private long countAssignedBranchesForEmployee(
            Employee employee
    ) {

        if (employee == null) {
            return 0L;
        }

        return branchService.findActiveBranchesForEmployeeId(
                        employee.getId()
                )
                .stream()
                .map(Branch::getId)
                .distinct()
                .count();
    }

    private BigDecimal sumAttendanceHoursForEmployeeToday(
            Employee employee
    ) {

        if (employee == null) {
            return BigDecimal.ZERO;
        }

        LocalDate today = LocalDate.now();

        return attendanceService.findSessionsByEmployeeId(
                        employee.getId()
                )
                .stream()
                .filter(session -> isSameDay(session.getCheckInTime(), today))
                .filter(session -> session.getWorkedHours() != null)
                .map(AttendanceSession::getWorkedHours)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }

    private BigDecimal sumAttendanceHoursForEmployeeCurrentMonth(
            Employee employee
    ) {

        if (employee == null) {
            return BigDecimal.ZERO;
        }

        LocalDate today = LocalDate.now();

        return attendanceService.findSessionsByEmployeeId(
                        employee.getId()
                )
                .stream()
                .filter(session -> isSameMonth(session.getCheckInTime(), today))
                .filter(session -> session.getWorkedHours() != null)
                .map(AttendanceSession::getWorkedHours)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }

    private long countAttendanceSessionsForEmployeeCurrentMonth(
            Employee employee
    ) {

        if (employee == null) {
            return 0L;
        }

        LocalDate today = LocalDate.now();

        return attendanceService.findSessionsByEmployeeId(
                        employee.getId()
                )
                .stream()
                .filter(session -> isSameMonth(session.getCheckInTime(), today))
                .count();
    }

    private String buildLatestPayrollSummary(
            Employee employee
    ) {

        if (employee == null) {
            return "No payroll profile available";
        }

        PayrollRecord latestRecord =
                payrollService.findPayrollRecordsByEmployeeId(
                                employee.getId()
                        )
                        .stream()
                        .sorted(
                                Comparator.comparingInt(PayrollRecord::getYear)
                                        .thenComparingInt(PayrollRecord::getMonth)
                                        .reversed()
                        )
                        .findFirst()
                        .orElse(null);

        if (latestRecord == null) {
            return "No payroll records available";
        }

        return "Latest payroll: "
                + latestRecord.getYear()
                + "-"
                + String.format("%02d", latestRecord.getMonth())
                + " | hours "
                + formatHours(latestRecord.getTotalHours())
                + " | salary "
                + formatMoney(latestRecord.getTotalSalary());
    }

    private long countTotalEmployees() {

        return employeeService.findAll().size();
    }

    private long countActiveEmployees() {

        return employeeService.findAll()
                .stream()
                .filter(Employee::isActive)
                .count();
    }

    private long countUniqueEmployeesAssignedToBranches() {

        return branchService.findAssignments()
                .stream()
                .filter(EmployeeBranchAssignment::isActive)
                .map(EmployeeBranchAssignment::getEmployeeId)
                .distinct()
                .count();
    }

    private long countEmployeesCurrentlyCheckedIn() {

        return attendanceService.findAllSessions()
                .stream()
                .filter(session -> session.getCheckInTime() != null)
                .filter(session -> session.getCheckOutTime() == null)
                .filter(session -> session.getBranchId() != null)
                .map(AttendanceSession::getEmployeeId)
                .distinct()
                .count();
    }

    private String buildAttendanceOverviewSummary() {

        long sessions =
                attendanceService.findAllSessions()
                        .stream()
                        .filter(this::isCurrentMonthSession)
                        .filter(session -> session.getBranchId() != null)
                        .count();

        BigDecimal hours =
                sumAttendanceHoursForCurrentMonthByBranch();

        return sessions
                + " sessions | "
                + formatDurationHours(hours);
    }

    private String buildInventoryOverviewSummary() {

        return formatMoney(inventoryService.findInventoryValue())
                + " value across "
                + inventoryService.findInventoryItemCount()
                + " items";
    }

    private String buildSalesOverviewSummary() {

        return formatMoney(saleService.findRevenueForCurrentMonth())
                + " revenue | "
                + saleService.countOrdersForCurrentMonth()
                + " orders";
    }

    private String buildPayrollTotalsSummary() {

        return "Current month "
                + formatMoney(payrollService.findPayrollCostForCurrentMonth())
                + " | total "
                + formatMoney(payrollService.findTotalPayrollCost());
    }

    private String buildRevenueTotalsSummary() {

        return "Current month "
                + formatMoney(saleService.findRevenueForCurrentMonth())
                + " | total "
                + formatMoney(saleService.findTotalRevenue());
    }

    private String buildInventoryAlertsSummary() {

        return String.valueOf(buildInventoryAlertRows().size())
                + " low stock items";
    }

    private List<DashboardInventoryAlertRow> buildInventoryAlertRows() {

        Map<Long, BigDecimal> quantitiesByProduct =
                inventoryService.findAllItems()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        item -> item.getProductId(),
                                        item -> BigDecimal.valueOf(item.getQuantity())
                                )
                        );

        Map<Long, String> productNamesById =
                inventoryService.findProductsById()
                        .entrySet()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        Map.Entry::getKey,
                                        entry -> entry.getValue().getName()
                                )
                        );

        return quantitiesByProduct.entrySet()
                .stream()
                .filter(entry -> entry.getValue().compareTo(BigDecimal.valueOf(5)) <= 0)
                .map(entry -> {
                    DashboardInventoryAlertRow row =
                            new DashboardInventoryAlertRow();

                    row.setProductName(
                            productNamesById.getOrDefault(
                                    entry.getKey(),
                                    "Unknown product"
                            )
                    );
                    row.setQuantity(entry.getValue().longValue());
                    row.setAlertText(
                            entry.getValue().compareTo(BigDecimal.ZERO) == 0
                                    ? "Out of stock"
                                    : "Low stock"
                    );

                    return row;
                })
                .sorted(
                        Comparator.comparing(
                                DashboardInventoryAlertRow::getProductName,
                                Comparator.nullsLast(String::compareToIgnoreCase)
                        )
                )
                .toList();
    }

    private List<DashboardAttendanceHistoryRow> buildRecentAttendanceHistory(
            Employee employee,
            int limit
    ) {

        if (employee == null) {
            return List.of();
        }

        Map<Long, String> branchesById =
                resolveEmployeeBranchNamesById(employee);

        return attendanceService.findSessionsByEmployeeId(employee.getId())
                .stream()
                .sorted(
                        Comparator.comparing(
                                AttendanceSession::getCheckInTime,
                                Comparator.nullsLast(Comparator.naturalOrder())
                        ).reversed()
                )
                .limit(limit)
                .map(session -> {
                    DashboardAttendanceHistoryRow row =
                            new DashboardAttendanceHistoryRow();

                    row.setDateLabel(
                            session.getCheckInTime() == null
                                    ? "-"
                                    : session.getCheckInTime().toLocalDate().toString()
                    );
                    row.setBranchName(
                            branchesById.getOrDefault(
                                    session.getBranchId(),
                                    "Unknown branch"
                            )
                    );
                    row.setCheckInTime(
                            session.getCheckInTime() == null
                                    ? "-"
                                    : TimeFormatUtil.formatTime(
                                            session.getCheckInTime().toLocalTime()
                                    )
                    );
                    row.setCheckOutTime(
                            session.getCheckOutTime() == null
                                    ? "Open"
                                    : TimeFormatUtil.formatTime(
                                            session.getCheckOutTime().toLocalTime()
                                    )
                    );
                    row.setWorkedHours(
                            session.getWorkedHours() == null
                                    ? "-"
                                    : formatDurationHours(session.getWorkedHours())
                    );

                    return row;
                })
                .toList();
    }

    private Map<Long, String> resolveEmployeeBranchNamesById(
            Employee employee
    ) {

        if (employee == null || employee.getId() == null) {
            return Map.of();
        }

        return branchService.findActiveBranchesForEmployeeId(
                        employee.getId()
                )
                .stream()
                .filter(branch -> branch.getId() != null)
                .collect(
                        Collectors.toMap(
                                Branch::getId,
                                branch -> {
                                    String name = branch.getName();
                                    return name == null ? "Unknown branch" : name;
                                },
                                (left, right) -> left
                        )
                );
    }

    private BigDecimal sumAttendanceHoursForCurrentMonthByBranch() {

        LocalDate now = LocalDate.now();
        LocalDate startDate = now.withDayOfMonth(1);
        LocalDate endDate = startDate.plusMonths(1);

        return attendanceService.findAllSessions()
                .stream()
                .filter(session -> session.getBranchId() != null)
                .filter(session -> session.getCheckInTime() != null)
                .filter(session ->
                        !session.getCheckInTime().isBefore(
                                startDate.atStartOfDay()
                        )
                                && session.getCheckInTime().isBefore(
                                endDate.atStartOfDay()
                        )
                )
                .filter(session -> session.getWorkedHours() != null)
                .map(AttendanceSession::getWorkedHours)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }

    private boolean isSameDay(
            LocalDateTime value,
            LocalDate date
    ) {

        if (value == null || date == null) {
            return false;
        }

        return value.getYear() == date.getYear()
                && value.getMonthValue() == date.getMonthValue()
                && value.getDayOfMonth() == date.getDayOfMonth();
    }

    private boolean isSameMonth(
            LocalDateTime value,
            LocalDate date
    ) {

        if (value == null || date == null) {
            return false;
        }

        return value.getYear() == date.getYear()
                && value.getMonthValue() == date.getMonthValue();
    }

    private String formatMoney(
            BigDecimal value
    ) {

        BigDecimal normalized =
                value == null
                        ? BigDecimal.ZERO
                        : value.setScale(2, RoundingMode.HALF_UP);

        return normalized.toPlainString();
    }

    private String formatHours(
            BigDecimal value
    ) {

        BigDecimal normalized =
                value == null
                        ? BigDecimal.ZERO
                        : value.setScale(2, RoundingMode.HALF_UP);

        return normalized.toPlainString();
    }

    private String formatDurationHours(
            BigDecimal value
    ) {

        return TimeFormatUtil.formatDurationHours(value);
    }

    private boolean isCurrentMonthSession(
            AttendanceSession session
    ) {

        if (session == null || session.getCheckInTime() == null) {
            return false;
        }

        LocalDate today = LocalDate.now();

        return session.getCheckInTime().getYear() == today.getYear()
                && session.getCheckInTime().getMonthValue() == today.getMonthValue();
    }

    private boolean isCurrentMonthPayrollRecord(
            PayrollRecord record
    ) {

        if (record == null) {
            return false;
        }

        LocalDate today = LocalDate.now();

        return record.getYear() == today.getYear()
                && record.getMonth() == today.getMonthValue();
    }
}
