package com.storemanager.domain.attendance.service;

import com.storemanager.core.security.PermissionGuard;
import com.storemanager.core.session.AppSession;
import com.storemanager.domain.attendance.model.AttendanceMonthlyTotal;
import com.storemanager.domain.attendance.model.AttendanceSession;
import com.storemanager.domain.attendance.repository.AttendanceRepository;
import com.storemanager.domain.branch.model.Branch;
import com.storemanager.domain.branch.service.BranchService;
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

    private final AttendanceRepository attendanceRepository =
            new AttendanceRepository();

    private final EmployeeService employeeService =
            new EmployeeService();

    private final BranchService branchService =
            new BranchService();

    public List<Employee> findEmployees() {

        validateAttendanceAccess();

        return employeeService.findAll();
    }

    public Map<Long, Employee> findEmployeesById() {

        validateAttendanceAccess();

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

        return branchService.findBranches();
    }

    public Map<Long, Branch> findBranchesById() {

        validateAttendanceAccess();

        return branchService.findBranchesById();
    }

    public List<AttendanceSession> findAllSessions() {

        validateAttendanceAccess();

        return attendanceRepository.findAllSessions();
    }

    public List<AttendanceMonthlyTotal> findCurrentMonthTotals() {

        validateAttendanceAccess();

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

    public boolean checkIn(
            Employee employee,
            Branch branch
    ) {

        validateAttendanceAccess();
        validateEmployee(employee);
        validateBranch(branch);
        validateEmployeeBranchAssignment(employee, branch);

        AttendanceSession openSession =
                attendanceRepository.findOpenSessionByEmployeeId(
                        employee.getId(),
                        branch.getId()
                );

        if (openSession != null) {
            throw new RuntimeException(
                    "Employee already has an open attendance session"
            );
        }

        AttendanceSession session =
                new AttendanceSession();

        session.setEmployeeId(employee.getId());
        session.setBranchId(branch.getId());
        session.setCheckInTime(LocalDateTime.now());
        session.setCreatedByUserId(getCurrentUserId());

        return attendanceRepository.checkIn(session);
    }

    public boolean checkOut(
            Employee employee,
            Branch branch
    ) {

        validateAttendanceAccess();
        validateEmployee(employee);
        validateBranch(branch);
        validateEmployeeBranchAssignment(employee, branch);

        AttendanceSession openSession =
                attendanceRepository.findOpenSessionByEmployeeId(
                        employee.getId(),
                        branch.getId()
                );

        if (openSession == null) {
            throw new RuntimeException(
                    "Employee has no open attendance session"
            );
        }

        LocalDateTime checkOutTime =
                LocalDateTime.now();

        openSession.setCheckOutTime(checkOutTime);
        openSession.setWorkedHours(
                calculateWorkedHours(
                        openSession.getCheckInTime(),
                        checkOutTime
                )
        );

        return attendanceRepository.checkOut(openSession);
    }

    private void validateAttendanceAccess() {

        if (!PermissionGuard.canViewEmployee()) {
            throw new RuntimeException(
                    "Attendance access denied"
            );
        }
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
            throw new RuntimeException(
                    "Employee is not assigned to this branch"
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

    private BigDecimal calculateWorkedHours(
            LocalDateTime checkInTime,
            LocalDateTime checkOutTime
    ) {

        if (checkInTime == null || checkOutTime == null) {
            return BigDecimal.ZERO;
        }

        long minutes =
                Duration
                        .between(
                                checkInTime,
                                checkOutTime
                        )
                        .toMinutes();

        if (minutes < 0) {
            throw new RuntimeException(
                    "Check-out time cannot be before check-in time"
            );
        }

        return BigDecimal
                .valueOf(minutes)
                .divide(
                        BigDecimal.valueOf(60),
                        2,
                        RoundingMode.HALF_UP
                );
    }
}
