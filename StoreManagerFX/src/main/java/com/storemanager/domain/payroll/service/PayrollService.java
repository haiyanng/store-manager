package com.storemanager.domain.payroll.service;

import com.storemanager.core.security.PermissionGuard;
import com.storemanager.core.session.AppSession;
import com.storemanager.domain.attendance.model.AttendanceMonthlyTotal;
import com.storemanager.domain.attendance.service.AttendanceService;
import com.storemanager.domain.employee.model.Employee;
import com.storemanager.domain.payroll.model.EmployeeSalaryConfig;
import com.storemanager.domain.payroll.model.PayrollRecord;
import com.storemanager.domain.payroll.repository.PayrollRepository;
import com.storemanager.domain.user.model.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class PayrollService {

    private final PayrollRepository payrollRepository =
            new PayrollRepository();

    private final AttendanceService attendanceService =
            new AttendanceService();

    public List<Employee> findEmployees() {

        validatePayrollAccess();

        return attendanceService.findEmployees();
    }

    public List<EmployeeSalaryConfig> findSalaryConfigs() {

        validatePayrollAccess();

        return payrollRepository.findAllSalaryConfigs();
    }

    public List<PayrollRecord> findPayrollRecords() {

        validatePayrollAccess();

        return payrollRepository.findPayrollRecords();
    }

    public boolean saveSalaryConfig(
            Employee employee,
            BigDecimal hourlyRate,
            boolean active
    ) {

        validatePayrollAccess();
        validateSalaryConfig(employee, hourlyRate);

        EmployeeSalaryConfig config =
                new EmployeeSalaryConfig();

        config.setEmployeeId(employee.getId());
        config.setHourlyRate(hourlyRate);
        config.setActive(active);

        return payrollRepository.saveOrUpdateSalaryConfig(config);
    }

    public int generatePayroll(
            int year,
            int month
    ) {

        validatePayrollAccess();
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
            }
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

    private void validatePayrollAccess() {

        if (!PermissionGuard.canViewEmployee()) {
            throw new RuntimeException(
                    "Payroll access denied"
            );
        }
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
}
