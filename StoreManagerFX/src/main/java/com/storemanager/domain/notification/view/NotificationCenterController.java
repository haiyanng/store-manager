package com.storemanager.domain.notification.view;

import com.storemanager.core.navigation.SceneManager;
import com.storemanager.core.session.AppSession;
import com.storemanager.domain.notification.model.Notification;
import com.storemanager.domain.notification.model.NotificationType;
import com.storemanager.domain.notification.presenter.NotificationCenterPresenter;
import com.storemanager.domain.user.model.User;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class NotificationCenterController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    private VBox rootPane;

    @FXML
    private Label statusLabel;

    @FXML
    private Label unreadLabel;

    @FXML
    private Button markAsReadButton;

    @FXML
    private TableView<Notification> recentTable;

    @FXML
    private TableColumn<Notification, String> recentTimeColumn;

    @FXML
    private TableColumn<Notification, String> recentTypeColumn;

    @FXML
    private TableColumn<Notification, String> recentTitleColumn;

    @FXML
    private TableColumn<Notification, String> recentReadColumn;

    @FXML
    private TableView<Notification> notificationTable;

    @FXML
    private TableColumn<Notification, String> createdAtColumn;

    @FXML
    private TableColumn<Notification, String> typeColumn;

    @FXML
    private TableColumn<Notification, String> titleColumn;

    @FXML
    private TableColumn<Notification, String> contentColumn;

    @FXML
    private TableColumn<Notification, String> readColumn;

    private final NotificationCenterPresenter presenter =
            new NotificationCenterPresenter(this);

    private boolean busy;

    private boolean markAsReadEnabled;

    @FXML
    public void initialize() {

        User currentUser = AppSession.getCurrentUser();
        if (currentUser == null) {
            SceneManager.switchScene("/fxml/auth/login.fxml");
            return;
        }

        configureTables();
        markAsReadButton.setDisable(true);
        attachLifecycle();
        presenter.initialize();
        presenter.startPolling();
    }

    @FXML
    public void onRefresh() {

        presenter.refreshNotifications();
    }

    @FXML
    public void onMarkAsRead() {

        presenter.markSelectedAsRead();
    }

    public void setNotifications(
            List<Notification> notifications
    ) {

        notificationTable.setItems(
                FXCollections.observableArrayList(notifications)
        );
        notificationTable.getSelectionModel().clearSelection();
        setMarkAsReadEnabled(false);
    }

    public void setRecentNotifications(
            List<Notification> notifications
    ) {

        recentTable.setItems(
                FXCollections.observableArrayList(notifications)
        );
    }

    public Long getSelectedNotificationId() {

        Notification selectedNotification =
                notificationTable.getSelectionModel().getSelectedItem();

        if (selectedNotification == null) {
            return null;
        }

        return selectedNotification.getId();
    }

    public void selectNotificationById(
            Long notificationId
    ) {

        if (notificationId == null) {
            notificationTable.getSelectionModel().clearSelection();
            return;
        }

        notificationTable.getItems()
                .stream()
                .filter(notification ->
                        notification.getId() != null
                                && notification.getId().equals(notificationId)
                )
                .findFirst()
                .ifPresent(notification ->
                        notificationTable.getSelectionModel().select(notification)
                );
    }

    public void setUnreadCount(
            long unreadCount
    ) {

        unreadLabel.setText(
                "Unread: " + unreadCount
        );
    }

    public void setMarkAsReadEnabled(
            boolean enabled
    ) {

        this.markAsReadEnabled = enabled;
        updateMarkButtonState();
    }

    public void setBusy(
            boolean busy
    ) {

        this.busy = busy;
        notificationTable.setDisable(busy);
        recentTable.setDisable(busy);
        updateMarkButtonState();
    }

    public void setStatus(
            String status
    ) {

        statusLabel.setText(status);
    }

    public void showError(
            String message
    ) {

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void configureTables() {

        notificationTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );
        recentTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );

        createdAtColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        formatDateTime(cellData.getValue().getCreatedAt())
                )
        );
        typeColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        safeEnum(cellData.getValue().getType())
                )
        );
        titleColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        cellData.getValue().getTitle()
                )
        );
        contentColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        cellData.getValue().getContent()
                )
        );
        readColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        cellData.getValue().isRead() ? "Yes" : "No"
                )
        );

        recentTimeColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        formatDateTime(cellData.getValue().getCreatedAt())
                )
        );
        recentTypeColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        safeEnum(cellData.getValue().getType())
                )
        );
        recentTitleColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        cellData.getValue().getTitle()
                )
        );
        recentReadColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        cellData.getValue().isRead() ? "Yes" : "No"
                )
        );

        notificationTable
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (observable, oldValue, newValue) ->
                                presenter.selectNotification(newValue)
                );
    }

    private String formatDateTime(
            LocalDateTime dateTime
    ) {

        if (dateTime == null) {
            return "";
        }

        return DATE_TIME_FORMATTER.format(dateTime);
    }

    private String safeEnum(
            NotificationType type
    ) {

        if (type == null) {
            return "";
        }

        return type.name();
    }

    private void attachLifecycle() {

        rootPane.sceneProperty().addListener(
                (observable, oldScene, newScene) -> {
                    if (newScene == null) {
                        presenter.stopPolling();
                    }
                }
        );
    }

    private void updateMarkButtonState() {

        markAsReadButton.setDisable(busy || !markAsReadEnabled);
    }
}
