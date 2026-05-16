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
    private Button userManagementButton;

    @FXML
    private Button productButton;

    @FXML
    private Button inventoryButton;

    @FXML
    private Button orderButton;

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
                "/fxml/employee/employee-placeholder.fxml"
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
                "/fxml/product/product-placeholder.fxml"
        );
    }

    @FXML
    public void onInventory() {

        ContentManager.loadContent(
                "/fxml/inventory/inventory-placeholder.fxml"
        );
    }

    @FXML
    public void onOrder() {

        ContentManager.loadContent(
                "/fxml/order/order-placeholder.fxml"
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
                userManagementButton,
                PermissionGuard.canViewUserManagement()
        );

        setButtonVisible(
                productButton,
                PermissionGuard.canViewProduct()
        );

        setButtonVisible(
                inventoryButton,
                PermissionGuard.canViewInventory()
        );

        setButtonVisible(
                orderButton,
                PermissionGuard.canViewOrder()
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
