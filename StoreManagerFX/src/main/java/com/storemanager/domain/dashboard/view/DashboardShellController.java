package com.storemanager.domain.dashboard.view;

import com.storemanager.core.navigation.ContentManager;
import com.storemanager.core.navigation.SceneManager;
import com.storemanager.core.security.PermissionGuard;
import com.storemanager.core.session.AppSession;
import com.storemanager.core.runtime.async.AsyncTaskRunner;
import com.storemanager.domain.auth.service.AuthService;
import com.storemanager.domain.branch.model.Branch;
import com.storemanager.domain.branch.service.BranchService;
import com.storemanager.domain.message.service.MessageService;
import com.storemanager.domain.notification.service.NotificationService;
import com.storemanager.domain.user.model.RoleType;
import com.storemanager.domain.user.model.User;
import javafx.collections.FXCollections;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.StringConverter;

import java.util.List;

public class DashboardShellController {

    @FXML
    private Label usernameLabel;

    @FXML
    private Label roleLabel;

    @FXML
    private Label activeBranchLabel;

    @FXML
    private ComboBox<Branch> activeBranchComboBox;

    @FXML
    private Button notificationButton;

    @FXML
    private Button messageButton;

    @FXML
    private Label notificationBadgeLabel;

    @FXML
    private Label messageBadgeLabel;

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
    private Button auditButton;

    @FXML
    private StackPane contentArea;

    private final NotificationService notificationService =
            new NotificationService();

    private final MessageService messageService =
            new MessageService();

    private final BranchService branchService =
            new BranchService();

    private Timeline badgeTimeline;

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

        initializeBranchContext();
        applySidebarPermissions();

        ContentManager.initialize(
                contentArea
        );

        ContentManager.loadContent(
                "/fxml/dashboard/dashboard-home.fxml"
        );

        refreshTopbarBadges();
        startBadgePolling();
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
    public void onAuditLogs() {

        ContentManager.loadContent(
                "/fxml/audit/audit-log-viewer.fxml"
        );
    }

    @FXML
    public void onNotifications() {

        ContentManager.loadContent(
                "/fxml/notification/notification-center.fxml"
        );
    }

    @FXML
    public void onMessages() {

        ContentManager.loadContent(
                "/fxml/message/message-inbox.fxml"
        );
    }

    @FXML
    public void onLogout() {

        stopBadgePolling();
        new AuthService().logout();

        SceneManager.switchScene(
                "/fxml/auth/login.fxml"
        );
    }

    @FXML
    public void onBranchSelected() {

        if (!branchService.canSwitchActiveBranch()) {
            return;
        }

        Branch selectedBranch =
                activeBranchComboBox.getValue();

        if (selectedBranch == null) {
            return;
        }

        setActiveBranch(selectedBranch);
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

        setButtonVisible(
                auditButton,
                PermissionGuard.canViewAuditLogs()
        );
    }

    private void initializeBranchContext() {

        List<Branch> branches =
                branchService.findOperationalBranches();

        configureBranchComboBox();

        Branch activeBranch =
                branchService.resolveActiveBranchContext();

        if (activeBranch == null && !branches.isEmpty()) {
            activeBranch = branches.get(0);
        }

        if (activeBranch != null) {
            AppSession.setActiveBranch(
                    activeBranch.getId(),
                    activeBranch.getName()
            );
        } else {
            AppSession.setActiveBranch(null, null);
        }

        activeBranchComboBox.setItems(
                FXCollections.observableArrayList(branches)
        );
        activeBranchComboBox.setDisable(
                !branchService.canSwitchActiveBranch()
        );
        activeBranchComboBox.setVisible(true);
        activeBranchComboBox.setManaged(true);

        if (activeBranch != null) {
            activeBranchComboBox.setValue(activeBranch);
        } else {
            activeBranchComboBox.setValue(null);
        }

        refreshActiveBranchLabel();
    }

    private void configureBranchComboBox() {

        activeBranchComboBox.setConverter(
                new StringConverter<>() {
                    @Override
                    public String toString(Branch branch) {
                        return branch == null ? "" : branch.getName();
                    }

                    @Override
                    public Branch fromString(String value) {
                        return null;
                    }
                }
        );

        activeBranchComboBox.setPromptText(
                "Select branch"
        );
    }

    private void setActiveBranch(
            Branch branch
    ) {

        if (branch == null) {
            return;
        }

        AppSession.setActiveBranch(
                branch.getId(),
                branch.getName()
        );
        refreshActiveBranchLabel();
    }

    private void refreshActiveBranchLabel() {

        String branchName =
                AppSession.getActiveBranchName();

        if (branchName == null || branchName.isBlank()) {
            activeBranchLabel.setText("🏢 Branch: Unassigned");
            return;
        }

        activeBranchLabel.setText(
                "🏢 Branch: " + branchName
        );
    }

    private void startBadgePolling() {

        if (badgeTimeline != null) {
            return;
        }

        badgeTimeline =
                new Timeline(
                        new KeyFrame(
                                Duration.seconds(15),
                                event -> refreshTopbarBadges()
                        )
                );
        badgeTimeline.setCycleCount(Timeline.INDEFINITE);
        badgeTimeline.play();
    }

    private void stopBadgePolling() {

        if (badgeTimeline != null) {
            badgeTimeline.stop();
            badgeTimeline = null;
        }
    }

    private void refreshTopbarBadges() {

        AsyncTaskRunner.run(
                () -> new BadgeSnapshot(
                        notificationService.countUnreadForCurrentUser(),
                        messageService.countUnreadForCurrentUser()
                ),
                snapshot -> {
                    updateBadge(
                            notificationBadgeLabel,
                            snapshot.notificationUnread()
                    );
                    updateBadge(
                            messageBadgeLabel,
                            snapshot.messageUnread()
                    );
                },
                throwable -> {
                    updateBadge(notificationBadgeLabel, 0);
                    updateBadge(messageBadgeLabel, 0);
                },
                () -> {
                }
        );
    }

    private void updateBadge(
            Label badgeLabel,
            long count
    ) {

        if (badgeLabel == null) {
            return;
        }

        badgeLabel.setText(
                Long.toString(count)
        );
        badgeLabel.setVisible(count > 0);
        badgeLabel.setManaged(count > 0);
    }

    private void setButtonVisible(
            Button button,
            boolean visible
    ) {

        button.setVisible(visible);
        button.setManaged(visible);
    }

    private record BadgeSnapshot(
            long notificationUnread,
            long messageUnread
    ) {
    }
}
