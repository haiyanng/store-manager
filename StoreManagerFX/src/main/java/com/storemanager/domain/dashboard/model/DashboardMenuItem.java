package com.storemanager.domain.dashboard.model;

import com.storemanager.domain.user.model.RoleType;

import java.util.Set;

public record DashboardMenuItem(
        String id,
        String label,
        String route,
        boolean sidebarItem,
        Set<RoleType> requiredRoles
) {

    public boolean isVisibleFor(
            RoleType role
    ) {

        if (role == null) {
            return false;
        }

        if (role == RoleType.DEVELOPER
                || role == RoleType.OWNER) {
            return true;
        }

        return requiredRoles.contains(role);
    }
}
