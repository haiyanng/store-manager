package com.storemanager.core.security;

import com.storemanager.core.session.AppSession;
import com.storemanager.domain.user.model.RoleType;
import com.storemanager.domain.user.model.User;

public class PermissionGuard {

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
        ) || hasRole(RoleType.STAFF);
    }

    public static boolean isStaff() {

        return isEmployee();
    }

    public static boolean isViewer() {

        return hasRole(
                RoleType.VIEWER
        );
    }

    public static boolean canViewDashboard() {

        return isOwner()
                || isManager()
                || isEmployee()
                || isViewer();
    }

    public static boolean canViewEmployee() {

        return isOwner()
                || isManager();
    }

    public static boolean canViewProduct() {

        return isOwner()
                || isManager()
                || isStaff()
                || isViewer();
    }

    public static boolean canModifyProduct() {

        return isOwner()
                || isManager();
    }

    public static boolean canViewInventory() {

        return isOwner()
                || isManager()
                || isStaff();
    }

    public static boolean canAdjustInventory() {

        // Shared by import and transactional stock workflows, not inventory UI actions.
        return isOwner()
                || isManager();
    }

    public static boolean canViewOrder() {

        return isOwner()
                || isManager()
                || isStaff();
    }

    public static boolean canCreateSale() {

        return isOwner()
                || isManager()
                || isStaff();
    }

    public static boolean canCancelSale() {

        return isOwner()
                || isManager();
    }

    public static boolean canViewOnlineOrders() {

        return isOwner()
                || isManager();
    }

    public static boolean canModifyOnlineOrders() {

        return canViewOnlineOrders();
    }

    public static boolean canCancelOnlineOrder() {

        return canModifyOnlineOrders();
    }

    public static boolean canRefundOnlineOrder() {

        return isOwner()
                || isManager();
    }

    public static boolean canViewCustomers() {

        return isOwner()
                || isManager()
                || isStaff()
                || isViewer();
    }

    public static boolean canViewReports() {

        return isOwner()
                || isManager()
                || isViewer();
    }

    public static boolean canViewUserManagement() {

        return canManageUsers();
    }

    public static boolean canManageUsers() {

        return isOwner();
    }

    public static boolean canAccessSystemTools() {

        return isOwner();
    }

    public static boolean canBackupDatabase() {

        return canAccessSystemTools();
    }

    public static boolean canRestoreDatabase() {

        return canAccessSystemTools();
    }

    public static boolean canViewAuditLogs() {

        return isOwner();
    }

    public static boolean canEditUser(
            User target
    ) {

        if (target == null) {
            return false;
        }

        return isOwner()
                && (
                target.getRole() == RoleType.MANAGER
                        || target.getRole() == RoleType.STAFF
                        || target.getRole() == RoleType.VIEWER
                        || target.getRole() == RoleType.EMPLOYEE
                        || target.getRole() == RoleType.CUSTOMER
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

        return isOwner()
                && (
                target.getRole() == RoleType.MANAGER
                        || target.getRole() == RoleType.STAFF
                        || target.getRole() == RoleType.VIEWER
                        || target.getRole() == RoleType.EMPLOYEE
                        || target.getRole() == RoleType.CUSTOMER
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

        return true;
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
