package com.storemanager.domain.user.service;

import com.storemanager.core.security.PasswordHasher;
import com.storemanager.core.security.PermissionGuard;
import com.storemanager.core.session.AppSession;
import com.storemanager.domain.audit.service.AuditService;
import com.storemanager.domain.employee.model.Employee;
import com.storemanager.domain.employee.repository.EmployeeRepository;
import com.storemanager.domain.user.model.RoleType;
import com.storemanager.domain.user.model.User;
import com.storemanager.domain.user.repository.UserRepository;

import java.util.List;

public class UserManagementService {

    private final UserRepository userRepository =
            new UserRepository();

    private final EmployeeRepository employeeRepository =
            new EmployeeRepository();

    private final AuditService auditService =
            new AuditService();

    public List<User> findAll() {

        validateUserManagementAccess();

        return userRepository.findAll();
    }

    public List<Employee> findEmployees() {

        validateUserManagementAccess();

        return employeeRepository.findAll();
    }

    public Employee findLinkedEmployee(
            User user
    ) {

        if (user == null || user.getId() == null) {
            return null;
        }

        return employeeRepository.findByUserId(
                user.getId()
        );
    }

    public void createUser(
            String username,
            String rawPassword,
            RoleType role
    ) {

        createUser(
                username,
                rawPassword,
                role,
                null
        );
    }

    public void createUser(
            String username,
            String rawPassword,
            RoleType role,
            Employee linkedEmployee
    ) {

        validateUserManagementAccess();

        if (role == null) {
            auditService.recordEvent(
                    "USER_MANAGEMENT",
                    "USER_CREATE",
                    "USER",
                    null,
                    false,
                    "Role is required",
                    "{\"username\":\"" + escape(username) + "\"}"
            );
            throw new RuntimeException(
                    "Role is required"
            );
        }

        if (username == null || username.trim().isEmpty()) {
            auditService.recordEvent(
                    "USER_MANAGEMENT",
                    "USER_CREATE",
                    "USER",
                    null,
                    false,
                    "Username is required",
                    "{}"
            );
            throw new RuntimeException(
                    "Username is required"
            );
        }

        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new RuntimeException(
                    "Password is required"
            );
        }

        if (userRepository.findByUsername(username.trim()).isPresent()) {
            auditService.recordEvent(
                    "USER_MANAGEMENT",
                    "USER_CREATE",
                    "USER",
                    null,
                    false,
                    "Username already exists",
                    "{\"username\":\"" + escape(username) + "\"}"
            );
            throw new RuntimeException(
                    "Username already exists"
            );
        }

        User user =
                new User();

        user.setUsername(
                username.trim()
        );

        user.setPassword(
                PasswordHasher.hash(rawPassword)
        );

        user.setRole(role);
        user.setActive(true);

        userRepository.save(user);

        User createdUser =
                userRepository
                        .findByUsername(
                                user.getUsername()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Created user cannot be loaded"
                                )
                        );

        updateEmployeeLink(
                createdUser,
                linkedEmployee
        );

        auditService.recordEvent(
                "USER_MANAGEMENT",
                "USER_CREATE",
                "USER",
                createdUser.getId(),
                true,
                null,
                "{\"username\":\"" + escape(createdUser.getUsername()) + "\",\"role\":\"" + createdUser.getRole() + "\"}"
        );
    }

    public boolean updateUser(
            User target,
            String username,
            String rawPassword,
            RoleType role
    ) {

        return updateUser(
                target,
                username,
                rawPassword,
                role,
                findLinkedEmployee(target)
        );
    }

    public boolean updateUser(
            User target,
            String username,
            String rawPassword,
            RoleType role,
            Employee linkedEmployee
    ) {

        if (target == null) {
            throw new RuntimeException(
                    "User is required"
            );
        }

        if (!PermissionGuard.canEditUser(target)) {
            auditService.recordPermissionDenied(
                    "USER_UPDATE",
                    "USER",
                    target.getId(),
                    "User edit denied",
                    null,
                    "OWNER"
            );
            throw new RuntimeException(
                    "Current user cannot edit this user"
            );
        }

        if (role == null) {
            throw new RuntimeException(
                    "Role is required"
            );
        }

        if (!PermissionGuard.canModifyRole(target, role)) {
            throw new RuntimeException(
                    "Current user cannot modify this role"
            );
        }

        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException(
                    "Username is required"
            );
        }

        var existingUser =
                userRepository.findByUsername(
                        username.trim()
                );

        if (existingUser.isPresent()
                && !existingUser.get().getId().equals(target.getId())) {
            throw new RuntimeException(
                    "Username already exists"
            );
        }

        RoleType oldRole =
                target.getRole();

        target.setUsername(
                username.trim()
        );

        if (rawPassword != null && !rawPassword.isEmpty()) {
            target.setPassword(
                    PasswordHasher.hash(rawPassword)
            );
        }

        target.setRole(role);

        boolean updated =
                userRepository.updateUser(target);

        if (!updated) {
            return false;
        }

        updateEmployeeLink(
                target,
                linkedEmployee
        );

        auditService.recordEvent(
                "USER_MANAGEMENT",
                oldRole == role ? "USER_UPDATE" : "USER_ROLE_CHANGE",
                "USER",
                target.getId(),
                true,
                null,
                "{\"username\":\"" + escape(target.getUsername()) + "\",\"role\":\"" + target.getRole() + "\"}"
        );

        return true;
    }

    public boolean deleteUser(
            User target
    ) {

        if (target == null) {
            throw new RuntimeException(
                    "User is required"
            );
        }

        User currentUser =
                AppSession.getCurrentUser();

        if (!PermissionGuard.canDeleteUser(target)) {
            auditService.recordPermissionDenied(
                    "USER_DELETE",
                    "USER",
                    target.getId(),
                    "User delete denied",
                    null,
                    "OWNER"
            );
            throw new RuntimeException(
                    "Current user cannot delete this user"
            );
        }

        if (currentUser != null
                && currentUser.getId().equals(target.getId())) {
            throw new RuntimeException(
                    "Cannot delete currently logged-in user"
            );
        }

        employeeRepository.clearUserLink(
                target.getId()
        );
        boolean deleted =
                userRepository.deleteUser(target);

        if (deleted) {
            auditService.recordEvent(
                    "USER_MANAGEMENT",
                    "USER_DELETE",
                    "USER",
                    target.getId(),
                    true,
                    null,
                    "{\"username\":\"" + escape(target.getUsername()) + "\"}"
            );
        }

        return deleted;
    }

    private void updateEmployeeLink(
            User user,
            Employee linkedEmployee
    ) {

        if (user == null || user.getId() == null) {
            throw new RuntimeException(
                    "User is required"
            );
        }

        if (linkedEmployee == null) {
            employeeRepository.clearUserLink(
                    user.getId()
            );
            return;
        }

        if (linkedEmployee.getUserId() != null
                && !linkedEmployee.getUserId().equals(user.getId())) {
            throw new RuntimeException(
                    "Employee is already linked to another account"
            );
        }

        boolean linked =
                employeeRepository.linkUserToEmployee(
                        user.getId(),
                        linkedEmployee.getId()
                );

        if (!linked) {
            throw new RuntimeException(
                    "Cannot link account to employee"
            );
        }
    }

    private void validateUserManagementAccess() {

        if (!PermissionGuard.canViewUserManagement()) {
            auditService.recordPermissionDenied(
                    "USER_MANAGEMENT",
                    "USER",
                    null,
                    "User management access denied",
                    null,
                    "OWNER"
            );
            throw new RuntimeException(
                    "Current user cannot manage users"
            );
        }
    }

    private String escape(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

}
