package com.storemanager.domain.user.service;

import com.storemanager.core.security.PasswordHasher;
import com.storemanager.core.security.PermissionGuard;
import com.storemanager.domain.audit.service.AuditService;
import com.storemanager.domain.audit.service.AuditSnapshots;
import com.storemanager.domain.employee.model.Employee;
import com.storemanager.domain.employee.repository.EmployeeRepository;
import com.storemanager.domain.user.model.RoleType;
import com.storemanager.domain.user.model.User;
import com.storemanager.domain.user.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class UserManagementService {
    private final UserRepository userRepository = new UserRepository();
    private final EmployeeRepository employeeRepository = new EmployeeRepository();
    private final AuditService auditService = new AuditService();

    public List<User> findAll() {
        validateAccess("USER_MANAGEMENT", null);
        return userRepository.findAll();
    }

    public List<Employee> findEmployees() {
        validateAccess("USER_MANAGEMENT", null);
        return employeeRepository.findAll();
    }

    public Employee findLinkedEmployee(User user) {
        validateAccess("USER_MANAGEMENT", user);
        return user == null || user.getId() == null ? null : employeeRepository.findByUserId(user.getId());
    }

    public void createUser(String username, String rawPassword, RoleType role) {
        createUser(username, rawPassword, role, null);
    }

    public void createUser(String username, String rawPassword, RoleType role, Employee employee) {
        validateAccess("USER_CREATE", null);
        User created = new User();
        Map<String, Object> attempted = AuditSnapshots.fields("username", username, "role", role,
                "employee_id", employee == null ? null : employee.getId());
        try {
            validateAccount(username, role);
            if (rawPassword == null || rawPassword.isEmpty()) throw new IllegalArgumentException("Password is required");
            if (userRepository.findByUsername(username.trim()).isPresent()) throw new IllegalArgumentException("Username already exists");
            validateEmployeeLink(null, employee);
            created.setUsername(username.trim());
            created.setPassword(PasswordHasher.hash(rawPassword));
            created.setRole(role);
            created.setActive(true);
            userRepository.save(created);
        } catch (RuntimeException e) {
            auditService.recordChange("USER_MANAGEMENT", "USER_CREATE", "USER", null,
                    false, e.getMessage(), null, null, attempted);
            throw e;
        }
        auditService.recordChange("USER_MANAGEMENT", "USER_CREATE", "USER", created.getId(),
                true, null, null, AuditSnapshots.user(created), null);
        linkAfterSave(created, employee);
    }

    public boolean updateUser(User target, String username, String rawPassword, RoleType role) {
        return updateUser(target, username, rawPassword, role, findLinkedEmployee(target));
    }

    public boolean updateUser(User target, String username, String rawPassword, RoleType role, Employee employee) {
        validateAccess("USER_UPDATE", target);
        Long id = target == null ? null : target.getId();
        Map<String, Object> before = null;
        boolean passwordChanged = rawPassword != null && !rawPassword.isEmpty();
        Map<String, Object> attempted = AuditSnapshots.fields("username", username, "role", role,
                "employee_id", employee == null ? null : employee.getId(), "password_changed", passwordChanged);
        User stored;
        boolean updated;
        String action = "USER_UPDATE";
        try {
            stored = requireStoredUser(id);
            before = AuditSnapshots.user(stored);
            if (!PermissionGuard.canEditUser(stored)) throw new IllegalArgumentException("You cannot edit this account");
            validateAccount(username, role);
            if (!PermissionGuard.canModifyRole(stored, role)) throw new IllegalArgumentException("You cannot change this account's role");
            var existing = userRepository.findByUsername(username.trim());
            if (existing.isPresent() && !existing.get().getId().equals(id)) throw new IllegalArgumentException("Username already exists");
            validateEmployeeLink(id, employee);
            action = stored.getRole() == role ? "USER_UPDATE" : "USER_ROLE_CHANGE";
            stored.setUsername(username.trim());
            stored.setRole(role);
            if (passwordChanged) stored.setPassword(PasswordHasher.hash(rawPassword));
            updated = userRepository.updateUser(stored);
        } catch (RuntimeException e) {
            auditService.recordChange("USER_MANAGEMENT", action, "USER", id,
                    false, e.getMessage(), before, before, attempted);
            throw e;
        }
        // Record the actor before changing an object that may also be the current session user.
        auditService.recordChange("USER_MANAGEMENT", action, "USER", id, updated,
                updated ? null : "Unable to save account changes", before,
                updated ? AuditSnapshots.user(stored) : before, attempted);
        if (!updated) return false;
        linkAfterSave(stored, employee);
        target.setUsername(stored.getUsername());
        target.setRole(stored.getRole());
        target.setPassword(stored.getPassword());
        return true;
    }

    public boolean deleteUser(User target) {
        validateAccess("USER_DELETE", target);
        Long id = target == null ? null : target.getId();
        Map<String, Object> before = null;
        boolean deleted;
        try {
            User stored = requireStoredUser(id);
            before = AuditSnapshots.user(stored);
            if (!PermissionGuard.canDeleteUser(stored)) throw new IllegalArgumentException("You cannot delete this account");
            // Unlink has its own event, even when the subsequent deletion fails.
            updateEmployeeLink(stored, null);
            deleted = userRepository.deleteUser(stored);
        } catch (RuntimeException e) {
            auditService.recordChange("USER_MANAGEMENT", "USER_DELETE", "USER", id,
                    false, e.getMessage(), before, before, null);
            throw e;
        }
        auditService.recordChange("USER_MANAGEMENT", "USER_DELETE", "USER", id, deleted,
                deleted ? null : "Unable to delete account", before, deleted ? null : before, null);
        return deleted;
    }

    private void linkAfterSave(User user, Employee employee) {
        try {
            updateEmployeeLink(user, employee);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Account was saved, but the employee link was not updated. Refresh the list before trying again. " + e.getMessage(), e);
        }
    }

    private void updateEmployeeLink(User user, Employee employee) {
        Map<String, Object> before = null;
        Long requestedId = employee == null ? null : employee.getId();
        Map<String, Object> attempted = AuditSnapshots.fields("employee_id", requestedId);
        boolean linked;
        try {
            Employee previous = employeeRepository.findByUserId(user.getId());
            Long previousId = previous == null ? null : previous.getId();
            before = AuditSnapshots.fields("employee_id", previousId);
            validateEmployeeLink(user.getId(), employee);
            if (Objects.equals(previousId, requestedId)) return;
            linked = employee == null ? employeeRepository.clearUserLink(user.getId())
                    : employeeRepository.linkUserToEmployee(user.getId(), requestedId);
        } catch (RuntimeException e) {
            auditService.recordChange("USER_MANAGEMENT", "USER_EMPLOYEE_LINK", "USER", user.getId(),
                    false, e.getMessage(), before, before, attempted);
            throw e;
        }
        auditService.recordChange("USER_MANAGEMENT", "USER_EMPLOYEE_LINK", "USER", user.getId(), linked,
                linked ? null : "Unable to update employee link", before, linked ? attempted : before,
                linked ? null : attempted);
        if (!linked) throw new IllegalStateException("Unable to update employee link");
    }

    private User requireStoredUser(Long id) {
        if (id == null) throw new IllegalArgumentException("Select an account first");
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account no longer exists. Refresh the list and try again."));
    }

    private void validateEmployeeLink(Long userId, Employee employee) {
        if (employee == null) return;
        if (employee.getId() == null) throw new IllegalArgumentException("Select an employee first");
        Employee actual = employeeRepository.findById(employee.getId());
        if (actual == null) throw new IllegalArgumentException("Selected employee no longer exists");
        if (actual.getUserId() != null && !actual.getUserId().equals(userId))
            throw new IllegalArgumentException("Employee is already linked to another account");
    }

    private void validateAccount(String username, RoleType role) {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("Username is required");
        if (role == null) throw new IllegalArgumentException("Role is required");
        if (role == RoleType.CUSTOMER) throw new IllegalArgumentException("Customer accounts cannot be created or assigned here");
    }

    private void validateAccess(String action, User target) {
        if (!PermissionGuard.canManageUsers()) {
            auditService.recordPermissionDenied(action, "USER", target == null ? null : target.getId(), "Account management access denied", "OWNER");
            throw new IllegalStateException("You do not have permission to manage accounts");
        }
    }
}
