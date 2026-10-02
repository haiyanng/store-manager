package com.storemanager.domain.attendance.service;

import com.storemanager.core.security.PermissionGuard;
import com.storemanager.core.session.AppSession;
import com.storemanager.core.util.TimeFormatUtil;
import com.storemanager.domain.audit.service.AuditService;
import com.storemanager.domain.attendance.model.*;
import com.storemanager.domain.attendance.repository.AttendanceRepository;
import com.storemanager.domain.attendance_anomaly.service.AttendanceAnomalyService;
import com.storemanager.domain.employee.model.Employee;
import com.storemanager.domain.employee.service.EmployeeService;
import com.storemanager.domain.user.model.User;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AttendanceService {
    private final AttendanceRepository repository = new AttendanceRepository();
    private final AttendanceAnomalyService anomalies = new AttendanceAnomalyService();
    private final EmployeeService employees = new EmployeeService();
    private final AuditService audit = new AuditService();

    public boolean isEmployeeSelfService() { return PermissionGuard.isStaff(); }

    public Employee getCurrentEmployee() {
        User user = AppSession.getCurrentUser();
        return user == null ? null : employees.findByUserId(user.getId());
    }

    public List<Employee> findEmployees() {
        validateAccess();
        if (!isEmployeeSelfService()) return employees.findAll();
        Employee employee = getCurrentEmployee();
        return employee == null ? List.of() : List.of(employee);
    }

    public Map<Long, Employee> findEmployeesById() {
        return findEmployees().stream().collect(Collectors.toMap(Employee::getId, e -> e));
    }

    public List<AttendanceSession> findAllSessions() {
        validateAccess();
        if (!isEmployeeSelfService()) return repository.findAllSessions();
        Employee employee = getCurrentEmployee();
        if (employee == null) return List.of();
        return repository.findAllSessions().stream()
                .filter(s -> employee.getId().equals(s.getEmployeeId())).toList();
    }

    public List<AttendanceSession> findSessionsByEmployeeId(Long employeeId) {
        return findAllSessions().stream().filter(s -> employeeId != null && employeeId.equals(s.getEmployeeId())).toList();
    }

    public List<AttendanceMonthlyTotal> findCurrentMonthTotals() {
        LocalDate now = LocalDate.now();
        return findMonthlyTotals(now.getYear(), now.getMonthValue());
    }

    public List<AttendanceMonthlyTotal> findMonthlyTotals(int year, int month) {
        validateAccess();
        List<AttendanceMonthlyTotal> totals = repository.findMonthlyTotals(LocalDate.of(year, month, 1));
        if (!isEmployeeSelfService()) return totals;
        Employee employee = getCurrentEmployee();
        return employee == null ? List.of() : totals.stream()
                .filter(t -> employee.getId().equals(t.getEmployeeId())).toList();
    }

    public AttendanceRuntimeStatus getCurrentRuntimeStatus() {
        validateAccess();
        AttendanceRuntimeStatus result = new AttendanceRuntimeStatus();
        result.setState(AttendanceState.NOT_WORKING);
        result.setStatusLabel("Not working");
        result.setTodayWorkedHours(BigDecimal.ZERO);
        Employee employee = getCurrentEmployee();
        result.setEmployeeLinked(employee != null && employee.isActive());
        if (!result.isEmployeeLinked()) {
            result.setStatusLabel("No active linked employee. Contact the owner.");
            return result;
        }
        List<AttendanceSession> sessions = findSessionsByEmployeeId(employee.getId());
        AttendanceSession open = sessions.stream().filter(s -> s.getCheckOutTime() == null).findFirst().orElse(null);
        result.setActiveSession(open);
        result.setLatestCheckInTime(sessions.stream().map(AttendanceSession::getCheckInTime)
                .filter(java.util.Objects::nonNull).max(LocalDateTime::compareTo).orElse(null));
        LocalDateTime now = TimeFormatUtil.truncateToSeconds(LocalDateTime.now());
        BigDecimal today = sessions.stream().filter(s -> s.getCheckInTime() != null
                && s.getCheckInTime().toLocalDate().equals(now.toLocalDate()))
                .map(s -> s.getCheckOutTime() == null ? hours(s.getCheckInTime(), now)
                        : s.getWorkedHours() == null ? BigDecimal.ZERO : s.getWorkedHours())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        result.setTodayWorkedHours(today);
        if (open != null) {
            result.setState(AttendanceState.WORKING);
            result.setStatusLabel("Currently working");
        }
        anomalies.scanOpenSessionsForMissingCheckout();
        return result;
    }

    public boolean checkIn(Employee employee) { return recordAttendance(employee, false); }
    public boolean checkOut(Employee employee) { return recordAttendance(employee, true); }
    public boolean checkInSelf() { return checkIn(getCurrentEmployee()); }
    public boolean checkOutSelf() { return checkOut(getCurrentEmployee()); }

    private boolean recordAttendance(Employee requested, boolean checkOut) {
        String action = checkOut ? AuditService.ACTION_ATTENDANCE_SELF_CHECK_OUT : AuditService.ACTION_ATTENDANCE_SELF_CHECK_IN;
        AttendanceSession session = null;
        boolean saved;
        try {
            validateAccess();
            Employee employee = resolveEmployee(requested);
            session = repository.findOpenSessionByEmployeeId(employee.getId());
            if (checkOut) {
                if (session == null) throw new IllegalStateException("No active attendance session found");
                LocalDateTime now = TimeFormatUtil.truncateToSeconds(LocalDateTime.now());
                if (now.isBefore(session.getCheckInTime())) throw new IllegalStateException("Check-out time cannot precede check-in time");
                session.setCheckOutTime(now);
                session.setWorkedHours(hours(session.getCheckInTime(), now));
                saved = repository.checkOut(session);
            } else {
                if (session != null) throw new IllegalStateException("Employee is already checked in");
                session = new AttendanceSession();
                session.setEmployeeId(employee.getId());
                session.setCreatedByUserId(AppSession.getCurrentUser().getId());
                session.setCheckInTime(TimeFormatUtil.truncateToSeconds(LocalDateTime.now()));
                saved = repository.checkIn(session);
            }
        } catch (RuntimeException e) {
            audit.recordEvent("ATTENDANCE", action, "ATTENDANCE_SESSION", session == null ? null : session.getId(),
                    false, e.getMessage(), "{}");
            throw e;
        }
        audit.recordEvent("ATTENDANCE", action, "ATTENDANCE_SESSION", session.getId(), saved,
                saved ? null : "Unable to save attendance", "{\"employee_id\":" + session.getEmployeeId() + "}");
        if (saved) {
            // The attendance write is already committed. A failed follow-up must not invite a duplicate retry.
            try {
                anomalies.evaluateSavedSession(session);
                anomalies.scanOpenSessionsForMissingCheckout();
            } catch (RuntimeException e) {
                audit.recordEvent("ATTENDANCE", "ANOMALY_EVALUATION", "ATTENDANCE_SESSION", session.getId(),
                        false, e.getMessage(), "{}");
            }
        }
        return saved;
    }

    private Employee resolveEmployee(Employee requested) {
        Employee target = requested;
        if (isEmployeeSelfService()) {
            target = getCurrentEmployee();
            if (target == null) throw new IllegalStateException("Your account is not linked to an employee. Contact the owner.");
            if (requested != null && !target.getId().equals(requested.getId()))
                throw new IllegalStateException("Staff can only manage their own attendance");
        }
        if (target == null || target.getId() == null) throw new IllegalArgumentException("Select an employee first");
        target = employees.findById(target.getId());
        if (target == null || !target.isActive()) throw new IllegalStateException("Employee is missing or inactive");
        return target;
    }

    private void validateAccess() {
        if (!(PermissionGuard.isOwner() || PermissionGuard.isManager() || PermissionGuard.isStaff()))
            throw new IllegalStateException("Attendance access denied");
    }

    private BigDecimal hours(LocalDateTime from, LocalDateTime to) {
        return BigDecimal.valueOf(Math.max(0, Duration.between(from, to).toSeconds()))
                .divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP);
    }
}
