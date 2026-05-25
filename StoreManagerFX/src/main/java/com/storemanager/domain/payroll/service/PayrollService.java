package com.storemanager.domain.payroll.service;

import com.storemanager.core.security.PermissionGuard;
import com.storemanager.core.session.AppSession;
import com.storemanager.domain.audit.service.AuditService;
import com.storemanager.domain.attendance.model.AttendanceMonthlyTotal;
import com.storemanager.domain.attendance.service.AttendanceService;
import com.storemanager.domain.employee.model.Employee;
import com.storemanager.domain.employee.service.EmployeeService;
import com.storemanager.domain.payroll.model.EmployeeSalaryConfig;
import com.storemanager.domain.payroll.model.PayrollRecord;
import com.storemanager.domain.payroll.repository.PayrollRepository;
import com.storemanager.domain.notification.model.NotificationType;
import com.storemanager.domain.notification.service.NotificationService;
import com.storemanager.domain.user.model.RoleType;
import com.storemanager.domain.user.model.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class PayrollService {

    private final PayrollRepository payrollRepository =
            new PayrollRepository();

    private final AttendanceService attendanceService =
            new AttendanceService();

    private final EmployeeService employeeService =
            new EmployeeService();

    private final NotificationService notificationService =
            new NotificationService();

    private final AuditService auditService =
            new AuditService();

    public List<Employee> findEmployees() {

        validatePayrollViewAccess();

        if (isEmployeeSelfService()) {
            Employee currentEmployee =
                    getCurrentEmployee();

            if (currentEmployee == null) {
                return List.of();
            }

            return List.of(currentEmployee);
        }

        return attendanceService.findEmployees();
    }

    public List<EmployeeSalaryConfig> findSalaryConfigs() {

        validatePayrollViewAccess();

        if (isEmployeeSelfService()) {
            Employee currentEmployee =
                    getCurrentEmployee();

            if (currentEmployee == null) {
                return List.of();
            }

            EmployeeSalaryConfig config =
                    payrollRepository.findActiveConfigByEmployeeId(
                            currentEmployee.getId()
                    );

            return config == null
                    ? List.of()
                    : List.of(config);
        }

        return payrollRepository.findAllSalaryConfigs();
    }

    public List<PayrollRecord> findPayrollRecords() {

        validatePayrollViewAccess();

        if (isEmployeeSelfService()) {
            Employee currentEmployee =
                    getCurrentEmployee();

            if (currentEmployee == null) {
                return List.of();
            }

            return payrollRepository.findPayrollRecordsByEmployeeId(
                    currentEmployee.getId()
            );
        }

        return payrollRepository.findPayrollRecords();
    }

    public List<PayrollRecord> findPayrollRecordsByEmployeeId(
            Long employeeId
    ) {

        validatePayrollViewAccess();

        if (isEmployeeSelfService()) {
            Employee currentEmployee =
                    getCurrentEmployee();

            if (currentEmployee == null
                    || !currentEmployee.getId().equals(employeeId)) {
                auditService.recordPermissionDenied(
                        AuditService.ACTION_PERMISSION_DENIED,
                        "PAYROLL",
                        employeeId,
                        "Payroll access denied",
                        null
                );
                throw new RuntimeException(
                        "Employees can only view their own payroll"
                );
            }
        }

        return payrollRepository.findPayrollRecordsByEmployeeId(employeeId);
    }

    public BigDecimal findTotalPayrollCost() {

        validatePayrollViewAccess();

        if (isEmployeeSelfService()) {
            return findPayrollRecords()
                    .stream()
                    .map(PayrollRecord::getTotalSalary)
                    .filter(value -> value != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        return payrollRepository.findTotalPayrollCost();
    }

    public BigDecimal findPayrollCostForCurrentMonth() {

        validatePayrollViewAccess();

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.withDayOfMonth(1);
        LocalDate endDate = startDate.plusMonths(1);

        if (isEmployeeSelfService()) {
            return findPayrollRecords()
                    .stream()
                    .filter(record ->
                            record.getYear() == today.getYear()
                                    && record.getMonth() == today.getMonthValue()
                    )
                    .map(PayrollRecord::getTotalSalary)
                    .filter(value -> value != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        return payrollRepository.findPayrollCostForPeriod(startDate, endDate);
    }

    public boolean saveSalaryConfig(
            Employee employee,
            BigDecimal hourlyRate,
            boolean active
    ) {

        validatePayrollManageAccess();
        validateSalaryConfig(employee, hourlyRate);

        EmployeeSalaryConfig config =
                new EmployeeSalaryConfig();

        config.setEmployeeId(employee.getId());
        config.setHourlyRate(hourlyRate);
        config.setActive(active);

        boolean updated =
                payrollRepository.saveOrUpdateSalaryConfig(config);

        if (updated) {
            auditService.record(
                    AuditService.ACTION_SALARY_CONFIG_UPDATED,
                    "SALARY_CONFIG",
                    employee.getId(),
                    "Salary configuration updated",
                    null
            );
            notifyEmployee(
                    employee,
                    "Salary updated",
                    "Your hourly rate was updated to "
                            + hourlyRate.toPlainString(),
                    NotificationType.PAYROLL
            );
        }

        return updated;
    }

    public int generatePayroll(
            int year,
            int month
    ) {

        validatePayrollManageAccess();
        validateMonth(year, month);

        List<AttendanceMonthlyTotal> monthlyTotals =
                attendanceService.findMonthlyTotals(
                        year,
                        month
                );

        int generatedCount =
                0;

        for (AttendanceMonthlyTotal monthlyTotal : monthlyTotals) {
            EmployeeSalaryConfig config =
                    payrollRepository.findActiveConfigByEmployeeId(
                            monthlyTotal.getEmployeeId()
                    );

            if (config == null) {
                continue;
            }

            PayrollRecord record =
                    buildPayrollRecord(
                            monthlyTotal,
                            config,
                            year,
                            month
                    );

            if (payrollRepository.savePayrollRecord(record)) {
                generatedCount++;
                notifyPayrollGenerated(monthlyTotal.getEmployeeId(), year, month, record);
            }
        }

        if (generatedCount > 0) {
            auditService.record(
                    AuditService.ACTION_PAYROLL_GENERATED,
                    "PAYROLL",
                    null,
                    "Generated payroll records: "
                            + generatedCount
                            + " for "
                            + year
                            + "-"
                            + String.format("%02d", month),
                    null
            );
        }

        return generatedCount;
    }

    public int getCurrentYear() {

        return LocalDate.now().getYear();
    }

    public int getCurrentMonth() {

        return LocalDate.now().getMonthValue();
    }

    private PayrollRecord buildPayrollRecord(
            AttendanceMonthlyTotal monthlyTotal,
            EmployeeSalaryConfig config,
            int year,
            int month
    ) {

        PayrollRecord record =
                new PayrollRecord();

        BigDecimal totalHours =
                monthlyTotal.getTotalWorkedHours() == null
                        ? BigDecimal.ZERO
                        : monthlyTotal.getTotalWorkedHours();

        BigDecimal hourlyRate =
                config.getHourlyRate();

        record.setEmployeeId(monthlyTotal.getEmployeeId());
        record.setYear(year);
        record.setMonth(month);
        record.setTotalHours(totalHours);
        record.setHourlyRateSnapshot(hourlyRate);
        record.setTotalSalary(
                totalHours.multiply(hourlyRate)
        );
        record.setGeneratedByUserId(getCurrentUserId());

        return record;
    }

    public boolean canManagePayroll() {

        return PermissionGuard.canViewEmployee();
    }

    public boolean isEmployeeSelfService() {

        User currentUser =
                AppSession.getCurrentUser();

        return currentUser != null
                && currentUser.getRole() == RoleType.EMPLOYEE;
    }

    private void validatePayrollViewAccess() {

        User currentUser =
                AppSession.getCurrentUser();

        if (currentUser == null) {
            auditService.recordPermissionDenied(
                    AuditService.ACTION_PERMISSION_DENIED,
                    "PAYROLL",
                    null,
                    "Payroll access denied",
                    null
            );
            throw new RuntimeException(
                    "Payroll access denied"
            );
        }
    }

    private void validatePayrollManageAccess() {

        if (!PermissionGuard.canViewEmployee()) {
            auditService.recordPermissionDenied(
                    AuditService.ACTION_PERMISSION_DENIED,
                    "PAYROLL",
                    null,
                    "Payroll access denied",
                    null
            );
            throw new RuntimeException(
                    "Payroll access denied"
            );
        }
    }

    private Employee getCurrentEmployee() {

        User currentUser =
                AppSession.getCurrentUser();

        if (currentUser == null) {
            return null;
        }

        return employeeService.findByUserId(currentUser.getId());
    }

    private void validateSalaryConfig(
            Employee employee,
            BigDecimal hourlyRate
    ) {

        if (employee == null || employee.getId() == null) {
            throw new RuntimeException(
                    "Employee is required"
            );
        }

        if (hourlyRate == null
                || hourlyRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException(
                    "Hourly rate cannot be negative"
            );
        }
    }

    private void validateMonth(
            int year,
            int month
    ) {

        if (year < 2000 || year > 2100) {
            throw new RuntimeException(
                    "Year is invalid"
            );
        }

        if (month < 1 || month > 12) {
            throw new RuntimeException(
                    "Month is invalid"
            );
        }
    }

    private Long getCurrentUserId() {

        User currentUser =
                AppSession.getCurrentUser();

        return currentUser == null
                ? null
                : currentUser.getId();
    }

    private void notifyPayrollGenerated(
            Long employeeId,
            int year,
            int month,
            PayrollRecord record
    ) {

        Employee employee = employeeService.findById(employeeId);
        if (employee == null) {
            return;
        }

        notifyEmployee(
                employee,
                "Payroll generated",
                "Payroll for "
                        + year
                        + "-"
                        + String.format("%02d", month)
                        + " was generated: "
                        + record.getTotalSalary().toPlainString(),
                NotificationType.PAYROLL
        );
    }

    private void notifyEmployee(
            Employee employee,
            String title,
            String content,
            NotificationType type
    ) {

        if (employee == null || employee.getUserId() == null) {
            return;
        }

        notificationService.notifyUser(
                employee.getUserId(),
                title,
                content,
                type
        );
    }
}
