package com.storemanager.domain.employee.service;

import com.storemanager.core.security.PermissionGuard;
import com.storemanager.domain.audit.service.AuditService;
import com.storemanager.domain.audit.service.AuditSnapshots;
import com.storemanager.domain.attendance.repository.AttendanceRepository;
import com.storemanager.domain.employee.model.EmployeeListViewDto;
import com.storemanager.domain.employee.model.Employee;
import com.storemanager.domain.employee.repository.EmployeeRepository;
import com.storemanager.domain.user.model.User;
import com.storemanager.domain.user.repository.UserRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EmployeeService {

    private final AuditService auditService = new AuditService();

    private final EmployeeRepository employeeRepository =
            new EmployeeRepository();

    private final AttendanceRepository attendanceRepository =
            new AttendanceRepository();

    private final UserRepository userRepository =
            new UserRepository();

    public List<Employee> findAll() {

        return employeeRepository.findAll();
    }

    public List<EmployeeListViewDto> findEmployeeListViews() {

        List<Employee> employees =
                findAll();

        Map<Long, User> usersById =
                userRepository.findAll()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        User::getId,
                                        Function.identity(),
                                        (left, right) -> left,
                                        LinkedHashMap::new
                                )
                        );

        return employees.stream()
                .map(employee -> toListView(employee, usersById))
                .toList();
    }

    public boolean create(Employee employee) {
        return change(employee, "EMPLOYEE_CREATE");
    }

    public boolean update(Employee employee) {
        return change(employee, "EMPLOYEE_UPDATE");
    }

    public boolean delete(Employee employee) {
        return change(employee, "EMPLOYEE_DELETE");
    }

    private boolean change(Employee employee, String action) {
        validateWriteAccess(action);
        Map<String, Object> before = null;
        Map<String, Object> attempted = AuditSnapshots.employee(employee);
        Map<String, Object> after;
        boolean success;
        try {
            if (employee == null) throw new IllegalArgumentException("Employee is required");
            boolean creating = action.endsWith("_CREATE");
            boolean deleting = action.endsWith("_DELETE");
            Employee stored = null;
            if (!creating) {
                if (employee.getId() == null) throw new IllegalArgumentException("Select an employee first");
                stored = employeeRepository.findById(employee.getId());
                if (stored == null) throw new IllegalArgumentException("Employee no longer exists. Refresh the list and try again.");
                before = AuditSnapshots.employee(stored);
            }
            Employee candidate = employee;
            if (!deleting) validate(candidate);
            attempted = deleting ? null : AuditSnapshots.employee(candidate);
            success = creating ? employeeRepository.save(candidate)
                    : deleting ? employeeRepository.delete(candidate) : employeeRepository.update(candidate);
            after = success ? (deleting ? null : AuditSnapshots.employee(candidate)) : before;
            if (success && !deleting) employee.setActive(candidate.isActive());
        } catch (RuntimeException e) {
            auditService.recordChange("EMPLOYEE", action, "EMPLOYEE", employee == null ? null : employee.getId(),
                    false, e.getMessage(), before, before, attempted);
            throw e;
        }
        auditService.recordChange("EMPLOYEE", action, "EMPLOYEE", employee.getId(), success,
                success ? null : "Unable to save employee changes. Check the data and database connection.",
                before, after, success ? null : attempted);
        return success;
    }

    private void validateWriteAccess(String action) {
        if (!PermissionGuard.canViewEmployee()) {
            auditService.recordPermissionDenied(action, "EMPLOYEE", null, "Employee management denied", "OWNER/MANAGER");
            throw new IllegalStateException("Current user cannot manage employees");
        }
    }

    public Map<Long, User> findUsersById() {

        return userRepository
                .findAll()
                .stream()
                .collect(
                        Collectors.toMap(
                                User::getId,
                                user -> user
                        )
                );
    }

    public Employee findByUserId(
            Long userId
    ) {

        if (userId == null) {
            return null;
        }

        return employeeRepository.findByUserId(userId);
    }

    public Employee findById(
            Long id
    ) {

        if (id == null) {
            return null;
        }

        return employeeRepository.findById(id);
    }

    private EmployeeListViewDto toListView(
            Employee employee,
            Map<Long, User> usersById
    ) {

        EmployeeListViewDto dto =
                new EmployeeListViewDto();

        if (employee == null) {

            return dto;
        }

        dto.setEmployeeId(employee.getId());
        dto.setFullName(employee.getFullName());
        dto.setPhone(employee.getPhone());
        dto.setPosition(employee.getPosition());
        dto.setActive(employee.isActive());

        User linkedUser =
                employee.getUserId() == null
                        ? null
                        : usersById.get(employee.getUserId());

        dto.setLinkedUsername(
                linkedUser == null || linkedUser.getUsername() == null
                        ? ""
                        : linkedUser.getUsername()
        );
        dto.setLinkedRole(
                linkedUser == null || linkedUser.getRole() == null
                        ? ""
                        : linkedUser.getRole().name()
        );

        return dto;
    }

    private void validate(
            Employee employee
    ) {

        if (employee == null) {
            throw new RuntimeException(
                    "Employee is required"
            );
        }

        if (employee.getFullName() == null
                || employee.getFullName().trim().isEmpty()) {
            throw new RuntimeException(
                    "Full name is required"
            );
        }

        if (employee.getPosition() == null
                || employee.getPosition().trim().isEmpty()) {
            throw new RuntimeException(
                    "Position is required"
            );
        }

        employee.setFullName(
                employee.getFullName().trim()
        );

        employee.setPhone(
                clean(employee.getPhone())
        );

        employee.setAddress(
                clean(employee.getAddress())
        );

        employee.setPosition(
                employee.getPosition().trim()
        );

        employee.setImagePath(
                cleanNullable(employee.getImagePath())
        );
    }

    private String clean(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value.trim();
    }

    private String cleanNullable(
            String value
    ) {

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }
}
