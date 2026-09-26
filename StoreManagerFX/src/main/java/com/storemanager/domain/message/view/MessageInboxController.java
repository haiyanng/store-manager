package com.storemanager.domain.message.view;

import com.storemanager.core.util.UiFeedback;
import com.storemanager.core.navigation.SceneManager;
import com.storemanager.core.session.AppSession;
import com.storemanager.domain.message.model.MessageConversationRow;
import com.storemanager.domain.message.model.MessageHistoryRow;
import com.storemanager.domain.message.presenter.MessageInboxPresenter;
import com.storemanager.domain.user.model.User;
import com.storemanager.core.util.TimeFormatUtil;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.util.List;

public class MessageInboxController {

    @FXML
    private VBox rootPane;

    @FXML
    private Label titleLabel;

    @FXML
    private Label subtitleLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Label conversationLabel;

    @FXML
    private Label unreadLabel;

    @FXML
    private TableView<MessageConversationRow> conversationTable;

    @FXML
    private TableColumn<MessageConversationRow, String> conversationUserColumn;

    @FXML
    private TableColumn<MessageConversationRow, String> conversationPreviewColumn;

    @FXML
    private TableColumn<MessageConversationRow, String> conversationTimeColumn;

    @FXML
    private TableColumn<MessageConversationRow, Long> conversationUnreadColumn;

    @FXML
    private TableView<MessageHistoryRow> historyTable;

    @FXML
    private TableColumn<MessageHistoryRow, String> historyTimeColumn;

    @FXML
    private TableColumn<MessageHistoryRow, String> historySenderColumn;

    @FXML
    private TableColumn<MessageHistoryRow, String> historyContentColumn;

    @FXML
    private TableColumn<MessageHistoryRow, String> historyReadColumn;

    @FXML
    private TextArea messageInput;

    @FXML
    private Button sendButton;

    @FXML
    private Button refreshButton;

    private final MessageInboxPresenter presenter =
            new MessageInboxPresenter(this);

    private boolean busy;

    private boolean sendEnabled;

    @FXML
    public void initialize() {
        UiFeedback.emptyTable(conversationTable, "No conversations found.");
        UiFeedback.emptyTable(historyTable, "No records found.");


        User currentUser = AppSession.getCurrentUser();
        if (currentUser == null) {
            SceneManager.switchScene("/fxml/auth/login.fxml");
            return;
        }

        configureTables();
        setSendEnabled(false);
        attachLifecycle();
        presenter.initialize();
        presenter.startPolling();
    }

    @FXML
    public void onRefresh() {

        presenter.refreshConversations();
        presenter.refreshHistory();
    }

    @FXML
    public void onSendMessage() {

        presenter.sendMessage(messageInput.getText());
    }

    public void setConversations(
            List<MessageConversationRow> conversations
    ) {

        conversationTable.setItems(
                FXCollections.observableArrayList(conversations)
        );
    }

    public void setMessages(
            List<MessageHistoryRow> rows
    ) {

        historyTable.setItems(
                FXCollections.observableArrayList(rows)
        );
    }

    public Long getSelectedConversationUserId() {

        MessageConversationRow selectedConversation =
                conversationTable.getSelectionModel().getSelectedItem();

        if (selectedConversation == null) {
            return null;
        }

        return selectedConversation.getOtherUserId();
    }

    public void selectConversationByUserId(
            Long userId
    ) {

        if (userId == null) {
            conversationTable.getSelectionModel().clearSelection();
            return;
        }

        conversationTable.getItems()
                .stream()
                .filter(row ->
                        row.getOtherUserId() != null
                                && row.getOtherUserId().equals(userId)
                )
                .findFirst()
                .ifPresent(row ->
                        conversationTable.getSelectionModel().select(row)
                );
    }

    public void setUnreadCount(
            long unreadCount
    ) {

        unreadLabel.setText(
                "Unread: " + unreadCount
        );
    }

    public void setConversationLabel(
            String value
    ) {

        conversationLabel.setText(value);
    }

    public void clearMessageInput() {

        messageInput.clear();
    }

    public void setBusy(
            boolean busy
    ) {

        this.busy = busy;
        refreshButton.setDisable(busy);
        conversationTable.setDisable(busy);
        historyTable.setDisable(busy);
        messageInput.setDisable(busy);
        updateSendButtonState();
    }

    public void setSendEnabled(
            boolean enabled
    ) {

        this.sendEnabled = enabled;
        updateSendButtonState();
    }

    public void setStatus(
            String status
    ) {

        UiFeedback.status(statusLabel, status, conversationTable, historyTable);
    }

    public void showError(
            String message
    ) {
        UiFeedback.showError(message);
    }

    private void configureTables() {

        conversationTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );
        historyTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );

        conversationUserColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        cellData.getValue().getOtherUsername()
                )
        );
        conversationPreviewColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        cellData.getValue().getLastMessage()
                )
        );
        conversationTimeColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        formatDateTime(
                                cellData.getValue().getLastMessageAt()
                        )
                )
        );
        conversationUnreadColumn.setCellValueFactory(
                cellData -> new SimpleObjectProperty<>(
                        cellData.getValue().getUnreadCount()
                )
        );

        historyTimeColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        formatDateTime(
                                cellData.getValue().getCreatedAt()
                        )
                )
        );
        historySenderColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        cellData.getValue().getSenderUsername()
                )
        );
        historyContentColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        cellData.getValue().getContent()
                )
        );
        historyReadColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        cellData.getValue().isRead() ? "Yes" : "No"
                )
        );

        conversationTable
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (observable, oldValue, newValue) ->
                                presenter.selectConversation(newValue)
                );
    }

    private String formatDateTime(
            LocalDateTime dateTime
    ) {

        return TimeFormatUtil.formatDateTime(dateTime);
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

    private void updateSendButtonState() {

        sendButton.setDisable(busy || !sendEnabled);
    }
}
