package com.storemanager.refactor;

import com.storemanager.core.security.PermissionGuard;
import com.storemanager.core.session.AppSession;
import com.storemanager.domain.dashboard.model.DashboardMenuRegistry;
import com.storemanager.domain.user.model.RoleType;
import com.storemanager.domain.user.model.User;
import org.junit.After;
import org.junit.Test;

import java.util.Locale;
import java.util.Set;

import static org.junit.Assert.*;

public class RoleAndNavigationTest {

    @After
    public void clearSession() {
        AppSession.clear();
    }

    @Test
    public void legacyCustomerRolesMapWithoutLocaleDependence() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals(RoleType.CUSTOMER, RoleType.fromDatabaseValue(" user "));
            assertEquals(RoleType.CUSTOMER, RoleType.fromDatabaseValue("customer"));
            assertEquals(RoleType.VIEWER, RoleType.fromDatabaseValue("viewer"));
            assertEquals(RoleType.STAFF, RoleType.fromDatabaseValue("EMPLOYEE"));
        } finally {
            Locale.setDefault(previous);
        }
        assertThrows(IllegalArgumentException.class, () -> RoleType.valueOf("DEVELOPER"));
        assertThrows(IllegalArgumentException.class, () -> RoleType.fromDatabaseValue(null));
        assertThrows(IllegalArgumentException.class, () -> RoleType.fromDatabaseValue("unknown"));
    }

    @Test
    public void survivingRolesKeepTheirBackOfficePermissions() {
        for (RoleType role : RoleType.values()) {
            User user = user(1L, role);
            AppSession.setCurrentUser(user);
            boolean administrative = Set.of(RoleType.OWNER, RoleType.MANAGER).contains(role);
            boolean staff = Set.of(RoleType.STAFF, RoleType.EMPLOYEE).contains(role);
            assertEquals(role.name(), role == RoleType.OWNER, PermissionGuard.canManageUsers());
            assertEquals(role.name(), administrative, PermissionGuard.canModifyProduct());
            assertEquals(role.name(), administrative || staff, PermissionGuard.canCreateSale());
            assertEquals(role.name(), administrative || staff, PermissionGuard.canViewInventory());
            assertEquals(role.name(), administrative, PermissionGuard.canCancelSale());
            assertEquals(role.name(), administrative || role == RoleType.VIEWER, PermissionGuard.canViewReports());
            assertEquals(role.name(), administrative, PermissionGuard.canAdjustInventory());
            assertFalse(role.name(), PermissionGuard.canDeleteUser(user));
        }
    }

    @Test
    public void customerCannotAccessBackOfficeAndOwnerCanManageCustomer() {
        User customer = user(2L, RoleType.fromDatabaseValue("USER"));
        AppSession.setCurrentUser(customer);
        assertTrue(AppSession.isLoggedIn());
        assertFalse(PermissionGuard.canViewDashboard());
        assertFalse(PermissionGuard.canAccessSystemTools());
        assertTrue(DashboardMenuRegistry.all().stream().noneMatch(item -> item.isVisibleFor(customer.getRole())));
        AppSession.setCurrentUser(user(1L, RoleType.OWNER));
        assertTrue(PermissionGuard.canEditUser(customer));
        assertTrue(PermissionGuard.canDeleteUser(customer));
        assertTrue(PermissionGuard.canModifyRole(customer, RoleType.CUSTOMER));
        AppSession.clear();
        assertFalse(PermissionGuard.canViewInventory());
        assertFalse(PermissionGuard.canManageUsers());
    }

    @Test
    public void everySurvivingMenuHasAResourceAndNoPayrollRoute() {
        for (var item : DashboardMenuRegistry.all()) {
            assertNotNull(item.route(), getClass().getResource(item.route()));
            assertFalse(item.route(), item.route().contains("payroll"));
            assertFalse(item.route(), item.route().contains("salary"));
        }
        var inventory = DashboardMenuRegistry.all().stream()
                .filter(item -> item.id().equals(DashboardMenuRegistry.INVENTORY))
                .findFirst().orElseThrow();
        assertEquals("View Inventory", inventory.label());
        assertNull(getClass().getResource("/fxml/inventory/inventory-adjustment-form.fxml"));
        assertNull(getClass().getResource("/fxml/payroll/payroll.fxml"));
    }

    static User user(long id, RoleType role) {
        User user = new User();
        user.setId(id);
        user.setUsername("test-" + id);
        user.setRole(role);
        user.setActive(true);
        return user;
    }
}
