package com.storemanager.domain.dashboard.view;

import com.storemanager.core.navigation.ContentManager;
import com.storemanager.core.navigation.SceneManager;
import com.storemanager.core.session.AppSession;
import com.storemanager.core.runtime.async.AsyncTaskRunner;
import com.storemanager.domain.dashboard.model.DashboardMenuItem;
import com.storemanager.domain.dashboard.model.DashboardMenuRegistry;
import com.storemanager.domain.dashboard.presenter.DashboardShellPresenter;
import com.storemanager.domain.message.service.MessageService;
import com.storemanager.domain.notification.service.NotificationService;
import com.storemanager.domain.user.model.RoleType;
import com.storemanager.domain.user.model.User;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class DashboardShellController {

    @FXML
    private Label usernameLabel;

    @FXML
    private Label roleLabel;

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
    private boolean logoutInProgress;

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
    public void onImport() {

        navigateTo(DashboardMenuRegistry.IMPORT);
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
        if (logoutInProgress) return;
        logoutInProgress = true;
        stopBadgePolling();
        var shell = contentArea.getScene().getRoot();
        shell.setDisable(true);
        presenter.logout(() -> SceneManager.switchScene("/fxml/auth/login.fxml"), error -> {
            logoutInProgress = false;
            shell.setDisable(false);
            startBadgePolling();
            com.storemanager.core.util.UiFeedback.showError(error.getMessage());
        });
    }

    @FXML public void onChangePassword() {
        javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Change password");
        dialog.initOwner(contentArea.getScene().getWindow());
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/typography.css").toExternalForm());
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/password-dialog.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("password-dialog");
        var current = new javafx.scene.control.PasswordField();
        var replacement = new javafx.scene.control.PasswordField();
        var confirmation = new javafx.scene.control.PasswordField();
        current.setPromptText("Current password"); replacement.setPromptText("New password");
        confirmation.setPromptText("Confirm new password");
        Label status = new Label(); status.setWrapText(true);
        var form = new VBox(12, new Label("Current password"), current,
                new Label("New password"), replacement, new Label("Confirm new password"), confirmation, status);
        form.setPrefWidth(360); dialog.getDialogPane().setContent(form);
        var save = new javafx.scene.control.ButtonType("Save password", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, javafx.scene.control.ButtonType.CANCEL);
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(save);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            event.consume(); saveButton.setDisable(true); status.setText("Saving password...");
            presenter.changePassword(current.getText(), replacement.getText(), confirmation.getText(), () -> {
                dialog.close();
                showPasswordChanged();
            }, error -> { saveButton.setDisable(false); status.setText(error.getMessage()); });
        });
        dialog.showAndWait();
    }

    private void showPasswordChanged() {
        Alert done = new Alert(Alert.AlertType.INFORMATION);
        done.initOwner(contentArea.getScene().getWindow());
        done.setTitle("Store Manager");
        done.setHeaderText("Password changed successfully");
        done.setContentText("Your password has been updated.\nUse your new password the next time you sign in.");
        done.getDialogPane().getStylesheets().add(getClass().getResource("/css/password-dialog.css").toExternalForm());
        done.getDialogPane().getStyleClass().addAll("password-dialog", "password-success-dialog");
        done.getDialogPane().setPrefWidth(460);
        Label icon = new Label("\u2713");
        icon.getStyleClass().add("password-success-icon");
        done.setGraphic(icon);
        done.showAndWait();
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
