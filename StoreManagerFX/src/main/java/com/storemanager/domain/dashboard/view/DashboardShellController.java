package com.storemanager.domain.dashboard.view;

import com.storemanager.core.navigation.ContentManager;
import com.storemanager.core.navigation.SceneManager;
import com.storemanager.core.security.PermissionGuard;
import com.storemanager.core.session.AppSession;
import com.storemanager.domain.user.model.RoleType;
import com.storemanager.domain.user.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class DashboardShellController {

    @FXML
    private Label usernameLabel;

    @FXML
    private Label roleLabel;

    @FXML
    private Button dashboardButton;

    @FXML
    private Button employeeButton;

    @FXML
    private Button attendanceButton;

    @FXML
    private Button payrollButton;

    @FXML
    private Button branchButton;

    @FXML
    private Button userManagementButton;

    @FXML
    private Button productButton;

    @FXML
    private Button categoryButton;

    @FXML
    private Button inventoryButton;

    @FXML
    private Button importButton;

    @FXML
    private Button orderButton;

    @FXML
    private Button systemToolsButton;

    @FXML
    private StackPane contentArea;

    @FXML
    public void initialize() {

        User currentUser =
                AppSession.getCurrentUser();

        if (currentUser == null) {

            SceneManager.switchScene(
                    "/fxml/auth/login.fxml"
            );

            return;
        }

        usernameLabel.setText(
                currentUser.getUsername()
        );

        RoleType currentRole =
                currentUser.getRole();

        roleLabel.setText(
                currentRole.name()
        );

        applySidebarPermissions();

        ContentManager.initialize(
                contentArea
        );

        ContentManager.loadContent(
                "/fxml/dashboard/dashboard-home.fxml"
        );
    }

    @FXML
    public void onDashboard() {

        ContentManager.loadContent(
                "/fxml/dashboard/dashboard-home.fxml"
        );
    }

    @FXML
    public void onEmployee() {

        ContentManager.loadContent(
                "/fxml/employee/employee-list.fxml"
        );
    }

    @FXML
    public void onAttendance() {

        ContentManager.loadContent(
                "/fxml/attendance/attendance.fxml"
        );
    }

    @FXML
    public void onPayroll() {

        ContentManager.loadContent(
                "/fxml/payroll/payroll.fxml"
        );
    }

    @FXML
    public void onBranch() {

        ContentManager.loadContent(
                "/fxml/branch/branch.fxml"
        );
    }

    @FXML
    public void onUserManagement() {

        ContentManager.loadContent(
                "/fxml/user/user-management.fxml"
        );
    }

    @FXML
    public void onProduct() {

        ContentManager.loadContent(
                "/fxml/product/product-list.fxml"
        );
    }

    @FXML
    public void onCategory() {

        ContentManager.loadContent(
                "/fxml/category/category-list.fxml"
        );
    }

    @FXML
    public void onInventory() {

        ContentManager.loadContent(
                "/fxml/inventory/inventory-list.fxml"
        );
    }

    @FXML
    public void onImport() {

        ContentManager.loadContent(
                "/fxml/importing/import.fxml"
        );
    }

    @FXML
    public void onOrder() {

        ContentManager.loadContent(
                "/fxml/sale/sale.fxml"
        );
    }

    @FXML
    public void onSystemTools() {

        ContentManager.loadContent(
                "/fxml/system_tool/system-tool.fxml"
        );
    }

    @FXML
    public void onLogout() {

        AppSession.clear();

        SceneManager.switchScene(
                "/fxml/auth/login.fxml"
        );
    }

    private void applySidebarPermissions() {

        setButtonVisible(
                dashboardButton,
                PermissionGuard.canViewDashboard()
        );

        setButtonVisible(
                employeeButton,
                PermissionGuard.canViewEmployee()
        );

        setButtonVisible(
                attendanceButton,
                PermissionGuard.canViewEmployee()
        );

        setButtonVisible(
                payrollButton,
                PermissionGuard.canViewEmployee()
        );

        setButtonVisible(
                branchButton,
                PermissionGuard.canViewEmployee()
        );

        setButtonVisible(
                userManagementButton,
                PermissionGuard.canViewUserManagement()
        );

        setButtonVisible(
                productButton,
                PermissionGuard.canViewProduct()
        );

        setButtonVisible(
                categoryButton,
                PermissionGuard.canViewProduct()
        );

        setButtonVisible(
                inventoryButton,
                PermissionGuard.canViewInventory()
        );

        setButtonVisible(
                importButton,
                PermissionGuard.canViewInventory()
        );

        setButtonVisible(
                orderButton,
                PermissionGuard.canViewOrder()
        );

        setButtonVisible(
                systemToolsButton,
                PermissionGuard.canAccessSystemTools()
        );
    }

    private void setButtonVisible(
            Button button,
            boolean visible
    ) {

        button.setVisible(visible);
        button.setManaged(visible);
    }
}
