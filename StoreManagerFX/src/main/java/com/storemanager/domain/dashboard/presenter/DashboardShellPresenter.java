package com.storemanager.domain.dashboard.presenter;

import com.storemanager.core.session.AppSession;
import com.storemanager.domain.dashboard.model.DashboardMenuItem;
import com.storemanager.domain.dashboard.model.DashboardMenuRegistry;
import com.storemanager.domain.user.model.RoleType;
import com.storemanager.domain.user.model.User;

import java.util.List;
import java.util.Optional;

public class DashboardShellPresenter {

    public List<DashboardMenuItem> getVisibleMenuItems() {

        RoleType role =
                getCurrentRole();

        return DashboardMenuRegistry.all()
                .stream()
                .filter(DashboardMenuItem::sidebarItem)
                .filter(menuItem -> menuItem.isVisibleFor(role))
                .toList();
    }

    public boolean canAccessMenuItem(
            String menuItemId
    ) {

        return findAccessibleMenuItem(menuItemId).isPresent();
    }

    public Optional<DashboardMenuItem> findAccessibleMenuItem(
            String menuItemId
    ) {

        if (menuItemId == null || menuItemId.isBlank()) {
            return Optional.empty();
        }

        RoleType role =
                getCurrentRole();

        return DashboardMenuRegistry.all()
                .stream()
                .filter(menuItem -> menuItem.id().equals(menuItemId))
                .filter(menuItem -> menuItem.isVisibleFor(role))
                .findFirst();
    }

    public void changePassword(String current, String replacement, String confirmation,
            Runnable success, java.util.function.Consumer<Throwable> failure) {
        com.storemanager.core.runtime.async.AsyncTaskRunner.run(() -> {
            new com.storemanager.domain.user.service.PasswordChangeService()
                    .changePassword(current, replacement, confirmation);
            return true;
        }, result -> success.run(), failure, null);
    }

    public void logout(Runnable success, java.util.function.Consumer<Throwable> failure) {
        com.storemanager.core.runtime.async.AsyncTaskRunner.run(() -> {
            new com.storemanager.domain.auth.service.AuthService().logout();
            return true;
        }, result -> success.run(), failure, null);
    }

    private RoleType getCurrentRole() {

        User currentUser =
                AppSession.getCurrentUser();

        return currentUser == null
                ? null
                : currentUser.getRole();
    }

}
