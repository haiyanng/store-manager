package com.storemanager.core.security;

import com.storemanager.core.session.AppSession;
import com.storemanager.domain.user.model.RoleType;
import com.storemanager.domain.user.model.User;

public class PermissionGuard {

    public static boolean isDeveloper() {

        return hasRole(
                RoleType.DEVELOPER
        );
    }

    public static boolean isOwner() {

        return hasRole(
                RoleType.OWNER
        );
    }

    public static boolean isManager() {

        return hasRole(
                RoleType.MANAGER
        );
    }

    public static boolean isEmployee() {

        return hasRole(
                RoleType.EMPLOYEE
        );
    }

    public static boolean canViewDashboard() {

        return isDeveloper()
                || isOwner()
                || isManager()
                || isEmployee();
    }

    public static boolean canViewEmployee() {

        return isDeveloper()
                || isOwner()
                || isManager();
    }

    public static boolean canViewProduct() {

        return isDeveloper()
                || isOwner();
    }

    public static boolean canViewInventory() {

        return isDeveloper()
                || isOwner()
                || isManager();
    }

    public static boolean canViewOrder() {

        return isDeveloper()
                || isOwner()
                || isManager()
                || isEmployee();
    }

    public static boolean canViewUserManagement() {

        return canManageUsers();
    }

    public static boolean canManageUsers() {

        return isDeveloper()
                || isOwner();
    }

    public static boolean isRootDeveloper(
            User user
    ) {

        return user != null
                && user.getRole() == RoleType.DEVELOPER;
    }

    public static boolean canEditUser(
            User target
    ) {

        if (target == null) {
            return false;
        }

        if (isDeveloper()) {
            return true;
        }

        return isOwner()
                && (
                target.getRole() == RoleType.MANAGER
                        || target.getRole() == RoleType.EMPLOYEE
        );
    }

    public static boolean canDeleteUser(
            User target
    ) {

        if (target == null) {
            return false;
        }

        User currentUser =
                AppSession.getCurrentUser();

        if (currentUser != null
                && currentUser.getId().equals(target.getId())) {
            return false;
        }

        if (isDeveloper()) {
            return true;
        }

        return isOwner()
                && (
                target.getRole() == RoleType.MANAGER
                        || target.getRole() == RoleType.EMPLOYEE
        );
    }

    public static boolean canModifyRole(
            User target,
            RoleType newRole
    ) {

        if (target == null || newRole == null) {
            return false;
        }

        if (!canEditUser(target)) {
            return false;
        }

        if (isRootDeveloper(target)) {
            return newRole == RoleType.DEVELOPER;
        }

        return newRole != RoleType.DEVELOPER;
    }

    private static boolean hasRole(
            RoleType role
    ) {

        User currentUser =
                AppSession.getCurrentUser();

        return currentUser != null
                && currentUser.getRole() == role;
    }
}
