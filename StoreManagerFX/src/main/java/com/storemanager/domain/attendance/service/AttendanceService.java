package com.storemanager.domain.attendance.service;

import com.storemanager.core.session.AppSession;
import com.storemanager.domain.audit.service.AuditService;
import com.storemanager.domain.attendance.model.AttendanceMonthlyTotal;
import com.storemanager.domain.attendance.model.AttendanceRuntimeStatus;
import com.storemanager.domain.attendance.model.AttendanceState;
import com.storemanager.domain.attendance.model.AttendanceSession;
import com.storemanager.domain.attendance.repository.AttendanceRepository;
import com.storemanager.domain.attendance_anomaly.service.AttendanceAnomalyService;
import com.storemanager.domain.branch.model.Branch;
import com.storemanager.domain.branch.service.BranchService;
import com.storemanager.domain.employee.model.Employee;
import com.storemanager.domain.employee.service.EmployeeService;
import com.storemanager.domain.notification.model.NotificationType;
import com.storemanager.domain.notification.service.NotificationService;
import com.storemanager.domain.user.model.User;
import com.storemanager.core.util.TimeFormatUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AttendanceService {

    private final AttendanceRepository attendanceRepository =
            new AttendanceRepository();

    private final AttendanceAnomalyService attendanceAnomalyService =
            new AttendanceAnomalyService();

    private final EmployeeService employeeService =
            new EmployeeService();

    private final BranchService branchService =
            new BranchService();

    private final NotificationService notificationService =
            new NotificationService();

    private final AuditService auditService =
            new AuditService();

    public List<Employee> findEmployees() {

        validateAttendanceAccess();

        if (isEmployeeSelfService()) {
            Employee currentEmployee =
                    getCurrentEmployee();

            if (currentEmployee == null) {
                return List.of();
            }

            return List.of(currentEmployee);
        }

        return employeeService.findAll();
    }

    public Map<Long, Employee> findEmployeesById() {

        validateAttendanceAccess();

        if (isEmployeeSelfService()) {
            Employee currentEmployee =
                    getCurrentEmployee();

            if (currentEmployee == null) {
                return Map.of();
            }

            return Map.of(
                    currentEmployee.getId(),
                    currentEmployee
            );
        }

        return employeeService
                .findAll()
                .stream()
                .collect(
                        Collectors.toMap(
                                Employee::getId,
                                employee -> employee
                        )
                );
    }

    public List<Branch> findBranches() {

        validateAttendanceAccess();

        if (isEmployeeSelfService()) {
            Employee currentEmployee =
                    getCurrentEmployee();

            if (currentEmployee == null) {
                return List.of();
            }

            return branchService.findActiveBranchesForEmployeeId(
                    currentEmployee.getId()
            );
        }

        return branchService.findBranches();
    }

    public Map<Long, Branch> findBranchesById() {

        validateAttendanceAccess();

        if (isEmployeeSelfService()) {
            return findBranches()
                    .stream()
                    .collect(
                            Collectors.toMap(
                                    Branch::getId,
                                    branch -> branch
                            )
                    );
        }

        return branchService.findBranchesById();
    }

    public List<AttendanceSession> findAllSessions() {

        validateAttendanceAccess();

        if (isEmployeeSelfService()) {
            Employee currentEmployee =
                    getCurrentEmployee();

            if (currentEmployee == null) {
                return List.of();
            }

            return attendanceRepository.findAllSessions()
                    .stream()
                    .filter(session ->
                            currentEmployee.getId().equals(
                                    session.getEmployeeId()
                            )
                    )
                    .toList();
        }

        return attendanceRepository.findAllSessions();
    }

    public List<AttendanceMonthlyTotal> findCurrentMonthTotals() {

        validateAttendanceAccess();

        if (isEmployeeSelfService()) {
            Employee currentEmployee =
                    getCurrentEmployee();

            if (currentEmployee == null) {
                return List.of();
            }

            return attendanceRepository.findMonthlyTotals(
                            LocalDate.now()
                    )
                    .stream()
                    .filter(total ->
                            currentEmployee.getId().equals(
                                    total.getEmployeeId()
                            )
                    )
                    .toList();
        }

        return attendanceRepository.findMonthlyTotals(
                LocalDate.now()
        );
    }

    public List<AttendanceMonthlyTotal> findMonthlyTotals(
            int year,
            int month
    ) {

        validateAttendanceAccess();

        return attendanceRepository.findMonthlyTotals(
                LocalDate.of(
                        year,
                        month,
                        1
                )
        );
    }

    public List<AttendanceSession> findSessionsByEmployeeId(
            Long employeeId
    ) {

        validateAttendanceAccess();

        return attendanceRepository.findAllSessions()
                .stream()
                .filter(session ->
                        employeeId != null
                                && employeeId.equals(session.getEmployeeId())
                )
                .toList();
    }

    public AttendanceRuntimeStatus getCurrentRuntimeStatus() {

        AttendanceRuntimeStatus status =
                new AttendanceRuntimeStatus();

        Employee currentEmployee =
                getCurrentEmployee();

        if (currentEmployee == null) {
            status.setState(AttendanceState.NOT_WORKING);
            status.setStatusLabel("Not working");
            status.setTodayWorkedHours(BigDecimal.ZERO);
            return status;
        }

        List<AttendanceSession> employeeSessions =
                attendanceRepository.findAllSessions()
                        .stream()
                        .filter(session ->
                                currentEmployee.getId().equals(
                                        session.getEmployeeId()
                                )
                        )
                        .toList();

        AttendanceSession activeSession =
                employeeSessions.stream()
                        .filter(session -> session.getCheckOutTime() == null)
                        .findFirst()
                        .orElse(null);

        status.setActiveSession(activeSession);
        status.setActiveBranchName(resolveBranchName(activeSession));
        status.setLatestCheckInTime(
                resolveLatestCheckInTime(employeeSessions, activeSession)
        );
        status.setTodayWorkedHours(
                calculateTodayWorkedHours(employeeSessions, activeSession)
        );

        if (activeSession != null) {
            status.setState(AttendanceState.WORKING);
            status.setStatusLabel("Currently Working");
            attendanceAnomalyService.scanOpenSessionsForMissingCheckout();
            return status;
        }

        status.setState(AttendanceState.NOT_WORKING);
        status.setStatusLabel("Not working");
        attendanceAnomalyService.scanOpenSessionsForMissingCheckout();
        return status;
    }

    public boolean isEmployeeSelfService() {

        User currentUser =
                AppSession.getCurrentUser();

        return currentUser != null
                && currentUser.getRole() == com.storemanager.domain.user.model.RoleType.EMPLOYEE;
    }

    public Employee getCurrentEmployee() {

        User currentUser =
                AppSession.getCurrentUser();

        if (currentUser == null) {
            return null;
        }

        return employeeService.findByUserId(currentUser.getId());
    }

    public boolean checkIn(
            Employee employee,
            Branch branch
    ) {

        validateAttendanceAccess();
        Employee targetEmployee =
                resolveTargetEmployee(employee);
        Branch targetBranch =
                resolveTargetBranch(branch);
        validateEmployee(targetEmployee);
        validateBranch(targetBranch);
        validateEmployeeBranchAssignment(targetEmployee, targetBranch);

        AttendanceSession openSession =
                attendanceRepository.findOpenSessionByEmployeeId(
                        targetEmployee.getId()
                );

        if (openSession != null) {
            auditDenied(
                    "Duplicate attendance check-in",
                    targetEmployee,
                    targetBranch
            );
            throw new RuntimeException("You are already checked in.");
        }

        AttendanceSession session =
                new AttendanceSession();

        session.setEmployeeId(targetEmployee.getId());
        session.setBranchId(targetBranch.getId());
        session.setCheckInTime(
                TimeFormatUtil.truncateToSeconds(
                        LocalDateTime.now()
                )
        );
        session.setCreatedByUserId(getCurrentUserId());

        boolean saved =
                attendanceRepository.checkIn(session);

        if (saved) {
            auditService.record(
                    AuditService.ACTION_ATTENDANCE_SELF_CHECK_IN,
                    "ATTENDANCE_SESSION",
                    targetEmployee.getId(),
                    "Self check-in at branch " + targetBranch.getName(),
                    targetBranch.getId()
            );
            attendanceAnomalyService.evaluateSavedSession(session);
            attendanceAnomalyService.scanOpenSessionsForMissingCheckout();
        }

        return saved;
    }

    public boolean checkInSelf() {

        return checkIn(null, null);
    }

    public boolean checkOut(
            Employee employee,
            Branch branch
    ) {

        validateAttendanceAccess();
        Employee targetEmployee =
                resolveTargetEmployee(employee);
        Branch targetBranch =
                resolveTargetBranch(branch);
        validateEmployee(targetEmployee);
        validateBranch(targetBranch);
        validateEmployeeBranchAssignment(targetEmployee, targetBranch);

        AttendanceSession openSession =
                attendanceRepository.findOpenSessionByEmployeeId(
                        targetEmployee.getId()
                );

        if (openSession == null) {
            auditDenied(
                    "Missing active attendance session",
                    targetEmployee,
                    targetBranch
            );
            throw new RuntimeException(
                    "No active attendance session found."
            );
        }

        if (openSession.getBranchId() != null
                && !openSession.getBranchId().equals(targetBranch.getId())) {
            auditDenied(
                    "Attendance branch mismatch",
                    targetEmployee,
                    targetBranch
            );
            throw new RuntimeException(
                    "Active attendance session belongs to another branch"
            );
        }

        LocalDateTime checkOutTime =
                TimeFormatUtil.truncateToSeconds(
                        LocalDateTime.now()
                );

        openSession.setCheckOutTime(checkOutTime);
        openSession.setWorkedHours(
                calculateWorkedHours(
                        openSession.getCheckInTime(),
                        checkOutTime
                )
        );

        boolean saved =
                attendanceRepository.checkOut(openSession);

        if (saved) {
            auditService.record(
                    AuditService.ACTION_ATTENDANCE_SELF_CHECK_OUT,
                    "ATTENDANCE_SESSION",
                    targetEmployee.getId(),
                    "Self check-out at branch " + targetBranch.getName(),
                    targetBranch.getId()
            );
            attendanceAnomalyService.evaluateSavedSession(openSession);
            attendanceAnomalyService.scanOpenSessionsForMissingCheckout();
        }

        return saved;
    }

    public boolean checkOutSelf() {

        return checkOut(null, null);
    }

    private void validateAttendanceAccess() {

        if (AppSession.getCurrentUser() == null) {
            notifyCurrentUser(
                    "Attendance denied",
                    "Attendance access was denied for your account",
                    NotificationType.ATTENDANCE
            );
            throw new RuntimeException(
                    "Attendance access denied"
            );
        }
    }

    private BigDecimal calculateTodayWorkedHours(
            List<AttendanceSession> employeeSessions,
            AttendanceSession activeSession
    ) {

        BigDecimal total =
                BigDecimal.ZERO;

        LocalDate today = LocalDate.now();

        for (AttendanceSession session : employeeSessions) {
            if (session.getCheckInTime() == null) {
                continue;
            }

            if (!session.getCheckInTime().toLocalDate().equals(today)) {
                continue;
            }

            if (session.getCheckOutTime() == null) {
                continue;
            }

            if (session.getWorkedHours() != null) {
                total = total.add(session.getWorkedHours());
            }
        }

        if (activeSession != null
                && activeSession.getCheckInTime() != null) {
            total = total.add(
                    calculateWorkedHours(
                            activeSession.getCheckInTime(),
                            TimeFormatUtil.truncateToSeconds(
                                    LocalDateTime.now()
                            )
                    )
            );
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private LocalDateTime resolveLatestCheckInTime(
            List<AttendanceSession> employeeSessions,
            AttendanceSession activeSession
    ) {

        if (activeSession != null) {
            return activeSession.getCheckInTime();
        }

        return employeeSessions.stream()
                .map(AttendanceSession::getCheckInTime)
                .filter(value -> value != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    private String resolveBranchName(
            AttendanceSession activeSession
    ) {

        if (activeSession == null || activeSession.getBranchId() == null) {
            String branchName =
                    AppSession.getActiveBranchName();

            return branchName == null ? "-" : branchName;
        }

        Branch branch =
                branchService.findBranchById(activeSession.getBranchId());

        if (branch == null || branch.getName() == null) {
            return "-";
        }

        return branch.getName();
    }

    private void validateEmployee(
            Employee employee
    ) {

        if (employee == null || employee.getId() == null) {
            throw new RuntimeException(
                    "Employee is required"
            );
        }
    }

    private void validateBranch(
            Branch branch
    ) {

        if (branch == null || branch.getId() == null) {
            throw new RuntimeException(
                    "Branch is required"
            );
        }

        if (!branch.isActive()) {
            throw new RuntimeException(
                    "Branch is inactive"
            );
        }
    }

    private void validateEmployeeBranchAssignment(
            Employee employee,
            Branch branch
    ) {

        if (!branchService.isEmployeeAssignedToBranch(
                employee.getId(),
                branch.getId()
        )) {
            notifyCurrentUser(
                    "Attendance denied",
                    "You can only check in at your assigned branch.",
                    NotificationType.ATTENDANCE
            );
            auditDenied(
                    "Employee not assigned to branch",
                    employee,
                    branch
            );
            throw new RuntimeException(
                    "You can only check in at your assigned branch."
            );
        }
    }

    private Employee resolveTargetEmployee(
            Employee employee
    ) {

        if (!isEmployeeSelfService()) {
            return employee;
        }

        Employee currentEmployee =
                getCurrentEmployee();

        if (currentEmployee == null) {
            throw new RuntimeException(
                    "Current employee account was not found"
            );
        }

        if (employee != null
                && employee.getId() != null
                && !currentEmployee.getId().equals(employee.getId())) {
            auditDenied(
                    "Attempted to act on another employee",
                    employee,
                    null
            );
            throw new RuntimeException(
                    "Employees can only manage their own attendance"
            );
        }

        return currentEmployee;
    }

    private Branch resolveTargetBranch(
            Branch branch
    ) {

        if (isEmployeeSelfService()) {
            Branch activeBranch =
                    resolveCurrentActiveBranchForSelfService();

            if (activeBranch == null) {
                throw new RuntimeException(
                        "No active branch selected."
                );
            }

            if (branch != null
                    && branch.getId() != null
                    && !branch.getId().equals(activeBranch.getId())) {
                auditDenied(
                        "Attempted attendance on a non-active branch",
                        getCurrentEmployee(),
                        branch
                );
            }

            return activeBranch;
        }

        if (branch != null && branch.getId() != null) {
            return branch;
        }

        Long activeBranchId =
                AppSession.getActiveBranchId();

        if (activeBranchId != null) {
            Branch activeBranch =
                    branchService.findBranchById(activeBranchId);

            if (activeBranch != null) {
                return activeBranch;
            }
        }

        if (isEmployeeSelfService()) {
            Employee currentEmployee =
                    getCurrentEmployee();

            if (currentEmployee == null) {
                return branch;
            }

            List<Branch> branches =
                    branchService.findActiveBranchesForEmployeeId(
                            currentEmployee.getId()
                    );

            if (!branches.isEmpty()) {
                return branches.get(0);
            }
        }

        return branch;
    }

    private Branch resolveCurrentActiveBranchForSelfService() {

        Long activeBranchId =
                AppSession.getActiveBranchId();

        if (activeBranchId != null) {
            Branch activeBranch =
                    branchService.findBranchById(activeBranchId);

            if (activeBranch != null) {
                return activeBranch;
            }
        }

        Employee currentEmployee =
                getCurrentEmployee();

        if (currentEmployee == null) {
            return null;
        }

        List<Branch> branches =
                branchService.findActiveBranchesForEmployeeId(
                        currentEmployee.getId()
                );

        if (branches.isEmpty()) {
            return null;
        }

        return branches.get(0);
    }

    private void auditDenied(
            String details,
            Employee employee,
            Branch branch
    ) {

        auditService.recordPermissionDenied(
                "ATTENDANCE_ATTEMPT",
                "ATTENDANCE_SESSION",
                employee == null ? null : employee.getId(),
                details,
                branch == null ? null : branch.getId()
        );
    }

    private Long getCurrentUserId() {

        User currentUser =
                AppSession.getCurrentUser();

        return currentUser == null
                ? null
                : currentUser.getId();
    }

    private BigDecimal calculateWorkedHours(
            LocalDateTime checkInTime,
            LocalDateTime checkOutTime
    ) {

        if (checkInTime == null || checkOutTime == null) {
            return BigDecimal.ZERO;
        }

        long workedSeconds =
                Duration
                        .between(
                                TimeFormatUtil.truncateToSeconds(checkInTime),
                                TimeFormatUtil.truncateToSeconds(checkOutTime)
                        )
                        .getSeconds();

        if (workedSeconds < 0) {
            throw new RuntimeException(
                    "Check-out time cannot be before check-in time"
            );
        }

        return BigDecimal
                .valueOf(workedSeconds)
                .divide(
                        BigDecimal.valueOf(3600L),
                        2,
                        RoundingMode.HALF_UP
                );
    }

    private void notifyCurrentUser(
            String title,
            String content,
            NotificationType type
    ) {

        User currentUser =
                AppSession.getCurrentUser();

        if (currentUser == null) {
            return;
        }

        notificationService.notifyUser(
                currentUser.getId(),
                title,
                content,
                type
        );
    }
}
