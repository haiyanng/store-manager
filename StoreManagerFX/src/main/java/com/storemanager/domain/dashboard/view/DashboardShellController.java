package com.storemanager.domain.dashboard.view;

import com.storemanager.core.navigation.ContentManager;
import com.storemanager.core.navigation.SceneManager;
import com.storemanager.core.session.AppSession;
import com.storemanager.core.runtime.async.AsyncTaskRunner;
import com.storemanager.domain.auth.service.AuthService;
import com.storemanager.domain.branch.model.Branch;
import com.storemanager.domain.dashboard.model.DashboardMenuItem;
import com.storemanager.domain.dashboard.model.DashboardMenuRegistry;
import com.storemanager.domain.dashboard.presenter.DashboardShellPresenter;
import com.storemanager.domain.message.service.MessageService;
import com.storemanager.domain.notification.service.NotificationService;
import com.storemanager.domain.user.model.RoleType;
import com.storemanager.domain.user.model.User;
import javafx.collections.FXCollections;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

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
    private VBox sidebarMenuBox;

    @FXML
    private StackPane contentArea;

    private final DashboardShellPresenter presenter =
            new DashboardShellPresenter();

    private final NotificationService notificationService =
            new NotificationService();

    private final MessageService messageService =
            new MessageService();

    private Timeline badgeTimeline;

    private boolean initializingBranchSelector;

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
        renderSidebarMenu();
        applyTopbarWorkflowVisibility();

        ContentManager.initialize(
                contentArea
        );

        navigateTo(DashboardMenuRegistry.DASHBOARD);

        refreshTopbarBadges();
        startBadgePolling();
    }

    @FXML
    public void onDashboard() {

        navigateTo(DashboardMenuRegistry.DASHBOARD);
    }

    @FXML
    public void onEmployee() {

        navigateTo(DashboardMenuRegistry.EMPLOYEE);
    }

    @FXML
    public void onAttendance() {

        navigateTo(DashboardMenuRegistry.ATTENDANCE);
    }

    @FXML
    public void onUserManagement() {

        navigateTo(DashboardMenuRegistry.USER_MANAGEMENT);
    }

    @FXML
    public void onProduct() {

        navigateTo(DashboardMenuRegistry.PRODUCT);
    }

    @FXML
    public void onCategory() {

        navigateTo(DashboardMenuRegistry.CATEGORY);
    }

    @FXML
    public void onInventory() {

        navigateTo(DashboardMenuRegistry.INVENTORY);
    }

    @FXML
    public void onImport() {

        navigateTo(DashboardMenuRegistry.IMPORT);
    }

    @FXML
    public void onOrder() {

        navigateTo(DashboardMenuRegistry.ORDER);
    }

    @FXML
    public void onSystemTools() {

        navigateTo(DashboardMenuRegistry.SYSTEM_TOOLS);
    }

    @FXML
    public void onAuditLogs() {

        navigateTo(DashboardMenuRegistry.AUDIT_LOGS);
    }

    @FXML
    public void onNotifications() {

        navigateTo(DashboardMenuRegistry.NOTIFICATIONS);
    }

    @FXML
    public void onMessages() {

        navigateTo(DashboardMenuRegistry.MESSAGES);
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

        if (initializingBranchSelector) {
            return;
        }

        Branch selectedBranch =
                activeBranchComboBox.getValue();

        if (selectedBranch == null) {
            return;
        }

        if (!presenter.canSwitchBranch(selectedBranch)) {
            showAccessDenied();
            return;
        }

        presenter.setActiveBranch(selectedBranch);
        refreshActiveBranchLabel();
    }

    private void initializeBranchContext() {

        configureBranchComboBox();

        DashboardShellPresenter.BranchSelectorState branchState =
                presenter.resolveBranchSelectorState();

        initializingBranchSelector = true;

        try {
            activeBranchComboBox.setItems(
                    FXCollections.observableArrayList(
                            branchState.branches()
                    )
            );
            activeBranchComboBox.setVisible(
                    branchState.visible()
            );
            activeBranchComboBox.setManaged(
                    branchState.visible()
            );
            activeBranchComboBox.setDisable(
                    !branchState.visible()
                            || !branchState.enabled()
            );

            if (branchState.selectedBranch() != null) {
                activeBranchComboBox.setValue(
                        branchState.selectedBranch()
                );
                presenter.setActiveBranch(
                        branchState.selectedBranch()
                );
            } else {
                activeBranchComboBox.setValue(null);
                presenter.setActiveBranch(null);
            }
        } finally {
            initializingBranchSelector = false;
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

        if (!presenter.canAccessMenuItem(DashboardMenuRegistry.NOTIFICATIONS)
                && !presenter.canAccessMenuItem(DashboardMenuRegistry.MESSAGES)) {
            return;
        }

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

    private void renderSidebarMenu() {

        sidebarMenuBox.getChildren().clear();

        for (DashboardMenuItem menuItem : presenter.getVisibleMenuItems()) {
            Button button =
                    new Button(menuItem.label());

            button.getStyleClass().add("nav-button");
            button.setMaxWidth(Double.MAX_VALUE);
            button.setContentDisplay(ContentDisplay.LEFT);
            button.setGraphicTextGap(10);
            button.setGraphic(createMenuIcon(menuItem.id()));
            button.setOnAction(event -> navigateTo(menuItem.id()));

            sidebarMenuBox.getChildren().add(button);
        }
    }

    private Label createMenuIcon(String menuItemId) {

        Label icon =
                new Label(resolveMenuIcon(menuItemId));
        icon.getStyleClass().add("nav-button-icon");
        icon.setMinWidth(22);
        icon.setMaxWidth(22);

        return icon;
    }

    private String resolveMenuIcon(String menuItemId) {

        return switch (menuItemId) {
            case DashboardMenuRegistry.DASHBOARD -> "◈";
            case DashboardMenuRegistry.EMPLOYEE -> "👥";
            case DashboardMenuRegistry.ATTENDANCE -> "◷";
            case DashboardMenuRegistry.ATTENDANCE_ANOMALIES -> "!";
            case DashboardMenuRegistry.USER_MANAGEMENT -> "⚙";
            case DashboardMenuRegistry.PRODUCT -> "▣";
            case DashboardMenuRegistry.CATEGORY -> "⌗";
            case DashboardMenuRegistry.INVENTORY -> "▤";
            case DashboardMenuRegistry.IMPORT -> "↧";
            case DashboardMenuRegistry.ORDER -> "◉";
            case DashboardMenuRegistry.ONLINE_ORDER -> "◎";
            case DashboardMenuRegistry.SYSTEM_TOOLS -> "⚒";
            case DashboardMenuRegistry.AUDIT_LOGS -> "☑";
            case DashboardMenuRegistry.NOTIFICATIONS -> "●";
            case DashboardMenuRegistry.MESSAGES -> "✉";
            default -> "•";
        };
    }

    private void applyTopbarWorkflowVisibility() {

        boolean notificationsVisible =
                presenter.canAccessMenuItem(DashboardMenuRegistry.NOTIFICATIONS);
        boolean messagesVisible =
                presenter.canAccessMenuItem(DashboardMenuRegistry.MESSAGES);

        setButtonVisible(notificationButton, notificationsVisible);
        setButtonVisible(messageButton, messagesVisible);

        if (!notificationsVisible) {
            updateBadge(notificationBadgeLabel, 0);
        }

        if (!messagesVisible) {
            updateBadge(messageBadgeLabel, 0);
        }
    }

    private void navigateTo(
            String menuItemId
    ) {

        presenter.findAccessibleMenuItem(menuItemId)
                .ifPresentOrElse(
                        menuItem -> ContentManager.loadContent(menuItem.route()),
                        this::showAccessDenied
                );
    }

    private void showAccessDenied() {

        Alert alert =
                new Alert(Alert.AlertType.ERROR);

        alert.setHeaderText(null);
        alert.setContentText("Access denied");
        alert.showAndWait();
    }

    private record BadgeSnapshot(
            long notificationUnread,
            long messageUnread
    ) {
    }
}
