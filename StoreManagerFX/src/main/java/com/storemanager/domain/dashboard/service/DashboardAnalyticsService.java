package com.storemanager.domain.dashboard.service;

import com.storemanager.core.security.PermissionGuard;
import com.storemanager.core.session.AppSession;
import com.storemanager.domain.attendance.model.AttendanceSession;
import com.storemanager.domain.attendance.service.AttendanceService;
import com.storemanager.domain.dashboard.model.DashboardAnalyticsSnapshot;
import com.storemanager.domain.dashboard.model.DashboardAttendanceHistoryRow;
import com.storemanager.domain.dashboard.model.DashboardCashFlowSummary;
import com.storemanager.domain.dashboard.model.DashboardInventoryAlertRow;
import com.storemanager.domain.dashboard.model.DashboardMetricCard;
import com.storemanager.domain.dashboard.model.EmployeeDashboardDto;
import com.storemanager.domain.dashboard.model.ManagerDashboardDto;
import com.storemanager.domain.dashboard.model.OwnerDashboardDto;
import com.storemanager.domain.employee.model.Employee;
import com.storemanager.domain.employee.service.EmployeeService;
import com.storemanager.domain.importing.service.ImportService;
import com.storemanager.domain.inventory.service.InventoryService;
import com.storemanager.domain.sale.service.SaleService;
import com.storemanager.domain.product.model.Product;
import com.storemanager.domain.product.service.ProductService;
import com.storemanager.domain.user.model.RoleType;
import com.storemanager.domain.user.model.User;
import com.storemanager.core.util.TimeFormatUtil;
import com.storemanager.core.util.MoneyFormatUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DashboardAnalyticsService {

    private final EmployeeService employeeService =
            new EmployeeService();

    private final AttendanceService attendanceService =
            new AttendanceService();

    private final SaleService saleService =
            new SaleService();

    private final ImportService importService =
            new ImportService();

    private final InventoryService inventoryService =
            new InventoryService();

    private final ProductService productService = new ProductService();

    public DashboardAnalyticsSnapshot loadDashboardAnalytics() {

        User currentUser =
                AppSession.getCurrentUser();

        if (currentUser == null) {
            throw new RuntimeException("User session is required");
        }

        if (!PermissionGuard.canViewDashboard()) {
            throw new RuntimeException("Dashboard access denied");
        }

        if (PermissionGuard.isViewer()) {
            return buildViewerSnapshot(currentUser);
        }

        if (PermissionGuard.isEmployee()) {
            return buildEmployeeSnapshot(currentUser);
        }

        if (PermissionGuard.isManager()) {
            return buildManagerSnapshot(currentUser);
        }

        return buildExecutiveSnapshot(currentUser);
    }

    private DashboardAnalyticsSnapshot buildViewerSnapshot(User currentUser) {
        List<Product> products = productService.findAllIncludingInactive();
        DashboardAnalyticsSnapshot snapshot = createBaseSnapshot(
                currentUser.getRole(), "Catalog Overview", "View products and categories");
        snapshot.setMetrics(List.of(
                new DashboardMetricCard("Products", String.valueOf(products.size()), "All catalog records"),
                new DashboardMetricCard("Active products",
                        String.valueOf(products.stream().filter(Product::isActive).count()), "Available catalog records"),
                new DashboardMetricCard("Categories", String.valueOf(productService.findCategories().size()),
                        "Product categories")
        ));
        return snapshot;
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
        dto.setSubtitle("Your attendance at this store");
        dto.setMetrics(buildSalesMetrics());
        dto.setTodayWorkedHours(
                formatHours(sumAttendanceHoursForEmployeeToday(employee))
        );
        dto.setCurrentMonthWorkedHours(
                formatHours(sumAttendanceHoursForEmployeeCurrentMonth(employee))
        );

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
                "Store attendance, inventory, and sales activity"
        );
        dto.setMetrics(buildManagerMetrics());

        dto.setEmployeesCurrentlyCheckedIn(countEmployeesCurrentlyCheckedIn());
        dto.setAttendanceOverview(buildAttendanceOverviewSummary());
        dto.setInventoryOverview(buildInventoryOverviewSummary());
        dto.setSalesOverview(buildSalesOverviewSummary());

        return dto;
    }

    private OwnerDashboardDto buildOwnerDashboard() {

        OwnerDashboardDto dto =
                new OwnerDashboardDto();

        dto.setTitle("Business Control");
        dto.setSubtitle(
                "Full business analytics, revenue totals, and business cash flow"
        );
        dto.setMetrics(buildOwnerMetrics());
        dto.setTotalEmployees(countTotalEmployees());
        dto.setActiveEmployees(countActiveEmployees());
        dto.setEmployeesCurrentlyWorking(countEmployeesCurrentlyCheckedIn());
        dto.setRevenueTotals(buildRevenueTotalsSummary());
        dto.setAttendanceOverview(formatDurationHours(sumAttendanceHoursForCurrentMonth()) + " worked this month");
        dto.setInventoryAlertsSummary(buildInventoryAlertsSummary());

        dto.setCashFlowSummaries(buildCashFlowSummaries());
        dto.setInventoryAlerts(buildInventoryAlertRows());

        return dto;
    }

    private List<DashboardMetricCard> buildEmployeeMetrics(Employee employee) {
        return List.of(
                new DashboardMetricCard("Today's hours", formatDurationHours(sumAttendanceHoursForEmployeeToday(employee)), "Worked today"),
                new DashboardMetricCard("Month hours", formatDurationHours(sumAttendanceHoursForEmployeeCurrentMonth(employee)), "Current month worked hours"));
    }

    private List<DashboardMetricCard> buildManagerMetrics() { return buildSalesMetrics(); }

    private List<DashboardMetricCard> buildOwnerMetrics() { return buildSalesMetrics(); }

    private List<DashboardMetricCard> buildSalesMetrics() {
        var items = inventoryService.findItemsWithExpiry();
        return List.of(
                new DashboardMetricCard("Today's sales", formatMoney(saleService.findRevenueToday()), "Sales revenue today"),
                new DashboardMetricCard("Today's orders", String.valueOf(saleService.countOrdersToday()), "Completed orders today"),
                new DashboardMetricCard("Low stock", String.valueOf(items.stream().filter(InventoryService::isLowStock).count()), "Products with fewer than 10 units"),
                new DashboardMetricCard("Expiring soon", String.valueOf(items.stream().filter(i -> i.getQuantity() > 0)
                        .filter(i -> "EXPIRING SOON".equals(i.getExpiryStatus())).count()), "Products expiring within 7 days"));
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

        DashboardCashFlowSummary monthSummary =
                new DashboardCashFlowSummary();
        monthSummary.setLabel("Current month");
        monthSummary.setRevenueTotal(revenueMonth);
        monthSummary.setImportCost(importMonth);
        monthSummary.setProductBusinessCashFlow(
                revenueMonth.subtract(importMonth)
        );

        DashboardCashFlowSummary totalSummary =
                new DashboardCashFlowSummary();
        totalSummary.setLabel("All time");
        totalSummary.setRevenueTotal(revenueAllTime);
        totalSummary.setImportCost(importAllTime);
        totalSummary.setProductBusinessCashFlow(
                revenueAllTime.subtract(importAllTime)
        );

        return List.of(monthSummary, totalSummary);
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

    private long countTotalEmployees() {

        return employeeService.findAll().size();
    }

    private long countActiveEmployees() {

        return employeeService.findAll()
                .stream()
                .filter(Employee::isActive)
                .count();
    }

    private long countEmployeesCurrentlyCheckedIn() {

        return attendanceService.findAllSessions()
                .stream()
                .filter(session -> session.getCheckInTime() != null)
                .filter(session -> session.getCheckOutTime() == null)
                .map(AttendanceSession::getEmployeeId)
                .distinct()
                .count();
    }

    private String buildAttendanceOverviewSummary() {

        long sessions =
                attendanceService.findAllSessions()
                        .stream()
                        .filter(this::isCurrentMonthSession)
                        .count();

        BigDecimal hours =
                sumAttendanceHoursForCurrentMonth();

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
                .filter(entry -> entry.getValue().compareTo(BigDecimal.TEN) < 0)
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

    private BigDecimal sumAttendanceHoursForCurrentMonth() {

        LocalDate now = LocalDate.now();
        LocalDate startDate = now.withDayOfMonth(1);
        LocalDate endDate = startDate.plusMonths(1);

        return attendanceService.findAllSessions()
                .stream()
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
        return MoneyFormatUtil.format(value);
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

}
