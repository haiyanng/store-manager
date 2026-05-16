package com.storemanager.domain.user.service;

import com.storemanager.core.security.PasswordHasher;
import com.storemanager.core.security.PermissionGuard;
import com.storemanager.core.session.AppSession;
import com.storemanager.domain.user.model.RoleType;
import com.storemanager.domain.user.model.User;
import com.storemanager.domain.user.repository.UserRepository;

import java.util.List;

public class UserManagementService {

    private final UserRepository userRepository =
            new UserRepository();

    public List<User> findAll() {

        return userRepository.findAll();
    }

    public void createUser(
            String username,
            String rawPassword,
            RoleType role
    ) {

        if (!PermissionGuard.canViewUserManagement()) {
            throw new RuntimeException(
                    "Current user cannot manage users"
            );
        }

        if (role == null) {
            throw new RuntimeException(
                    "Role is required"
            );
        }

        if (role == RoleType.DEVELOPER) {
            if (countDevelopers() >= 1) {
                throw new RuntimeException(
                        "Only one developer account can exist"
                );
            }

            throw new RuntimeException(
                    "Cannot create developer users from UI"
            );
        }

        if (username == null || username.trim().isEmpty()) {
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
    }

    public boolean updateUser(
            User target,
            String username,
            String rawPassword,
            RoleType role
    ) {

        if (target == null) {
            throw new RuntimeException(
                    "User is required"
            );
        }

        if (!PermissionGuard.canEditUser(target)) {
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
            if (PermissionGuard.isRootDeveloper(target)) {
                throw new RuntimeException(
                        "Cannot change developer role"
                );
            }

            if (role == RoleType.DEVELOPER) {
                throw new RuntimeException(
                        "Cannot promote users to developer from UI"
                );
            }

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

        target.setUsername(
                username.trim()
        );

        if (rawPassword != null && !rawPassword.isEmpty()) {
            target.setPassword(
                    PasswordHasher.hash(rawPassword)
            );
        }

        target.setRole(role);

        return userRepository.updateUser(target);
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

        if (currentUser != null
                && PermissionGuard.isRootDeveloper(currentUser)
                && currentUser.getId().equals(target.getId())) {
            throw new RuntimeException(
                    "Developer cannot delete their own account"
            );
        }

        if (!PermissionGuard.canDeleteUser(target)) {
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

        if (target.getRole() == RoleType.DEVELOPER
                && countDevelopers() <= 1) {
            throw new RuntimeException(
                    "Cannot delete the last developer account"
            );
        }

        return userRepository.deleteUser(target);
    }

    private long countDevelopers() {

        return userRepository
                .findAll()
                .stream()
                .filter(user -> user.getRole() == RoleType.DEVELOPER)
                .count();
    }
}
