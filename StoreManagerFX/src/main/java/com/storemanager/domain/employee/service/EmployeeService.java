package com.storemanager.domain.employee.service;

import com.storemanager.core.session.AppSession;
import com.storemanager.domain.attendance.model.AttendanceSession;
import com.storemanager.domain.attendance.repository.AttendanceRepository;
import com.storemanager.domain.branch.model.Branch;
import com.storemanager.domain.branch.repository.BranchRepository;
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

    private final EmployeeRepository employeeRepository =
            new EmployeeRepository();

    private final BranchRepository branchRepository =
            new BranchRepository();

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

    public boolean create(
            Employee employee
    ) {

        validate(employee);

        return employeeRepository.save(employee);
    }

    public boolean update(
            Employee employee
    ) {

        if (employee.getId() == null) {
            throw new RuntimeException(
                    "Employee is required"
            );
        }

        validate(employee);

        return employeeRepository.update(employee);
    }

    public boolean delete(
            Employee employee
    ) {

        if (employee == null || employee.getId() == null) {
            throw new RuntimeException(
                    "Employee is required"
            );
        }

        return employeeRepository.delete(employee);
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
            dto.setBranchDisplayName("Unassigned");
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
        dto.setBranchDisplayName(
                resolveBranchDisplayName(employee)
        );

        return dto;
    }

    private String resolveBranchDisplayName(
            Employee employee
    ) {

        if (employee == null || employee.getId() == null) {
            return "Unassigned";
        }

        AttendanceSession activeSession =
                attendanceRepository.findOpenSessionByEmployeeId(
                        employee.getId()
                );

        if (activeSession != null
                && activeSession.getBranchId() != null) {
            String branchName =
                    resolveBranchName(activeSession.getBranchId());
            return branchName + " (Working)";
        }

        Branch currentBranch =
                resolveCurrentAssignedBranch(employee.getId());

        if (currentBranch != null) {
            return currentBranch.getName();
        }

        return "Unassigned";
    }

    private Branch resolveCurrentAssignedBranch(
            Long employeeId
    ) {

        if (employeeId == null) {
            return null;
        }

        var activeAssignment =
                branchRepository.findActiveAssignmentByEmployeeId(
                        employeeId
                );

        if (activeAssignment == null
                || activeAssignment.getBranchId() == null) {
            return null;
        }

        Branch branch =
                branchRepository.findBranchById(
                        activeAssignment.getBranchId()
                );

        if (branch == null || !branch.isActive()) {
            return null;
        }

        return branch;
    }

    private String resolveBranchName(
            Long branchId
    ) {

        if (branchId == null) {
            return "Unassigned";
        }

        Branch branch =
                branchRepository.findBranchById(branchId);

        if (branch == null || branch.getName() == null
                || branch.getName().isBlank()) {
            return "Branch #" + branchId;
        }

        return branch.getName();
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
