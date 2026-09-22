package com.storemanager.domain.onlineorder.view;

import com.storemanager.core.security.PermissionGuard;
import com.storemanager.core.util.TimeFormatUtil;
import com.storemanager.domain.onlineorder.model.OnlineOrder;
import com.storemanager.domain.onlineorder.model.OnlineOrderDetail;
import com.storemanager.domain.onlineorder.model.OnlineOrderHistoryEntry;
import com.storemanager.domain.onlineorder.model.OnlineOrderItem;
import com.storemanager.domain.onlineorder.model.OnlineOrderStatus;
import com.storemanager.domain.onlineorder.model.OnlineOrderSummary;
import com.storemanager.domain.onlineorder.presenter.OnlineOrderPresenter;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class OnlineOrderController {

    @FXML
    private TableView<OnlineOrderSummary> orderTable;

    @FXML
    private TableColumn<OnlineOrderSummary, Long> orderIdColumn;

    @FXML
    private TableColumn<OnlineOrderSummary, String> orderCustomerColumn;

    @FXML
    private TableColumn<OnlineOrderSummary, String> orderEmailColumn;

    @FXML
    private TableColumn<OnlineOrderSummary, String> orderStatusColumn;

    @FXML
    private TableColumn<OnlineOrderSummary, BigDecimal> orderTotalColumn;

    @FXML
    private TableColumn<OnlineOrderSummary, String> orderCreatedAtColumn;

    @FXML
    private Label orderIdValueLabel;

    @FXML
    private Label orderStatusValueLabel;

    @FXML
    private Label orderTotalValueLabel;

    @FXML
    private Label orderCreatedAtValueLabel;

    @FXML
    private Label customerFullNameValueLabel;

    @FXML
    private Label customerEmailValueLabel;

    @FXML
    private Label recipientNameValueLabel;

    @FXML
    private Label phoneValueLabel;

    @FXML
    private Label shippingAddressValueLabel;

    @FXML
    private Label paymentMethodValueLabel;

    @FXML
    private Button confirmButton;

    @FXML
    private Button preparingButton;

    @FXML
    private Button readyButton;

    @FXML
    private Button deliveringButton;

    @FXML
    private Button deliveredButton;

    @FXML
    private Button completedButton;

    @FXML
    private Button refundButton;

    @FXML
    private Button rejectButton;

    @FXML
    private Button cancelButton;

    @FXML
    private Button refreshButton;

    @FXML
    private TableView<OnlineOrderItem> itemTable;

    @FXML
    private TableColumn<OnlineOrderItem, String> itemProductColumn;

    @FXML
    private TableColumn<OnlineOrderItem, Integer> itemQuantityColumn;

    @FXML
    private TableColumn<OnlineOrderItem, BigDecimal> itemUnitPriceColumn;

    @FXML
    private TableColumn<OnlineOrderItem, BigDecimal> itemSubtotalColumn;

    @FXML
    private TableView<OnlineOrderHistoryEntry> historyTable;

    @FXML
    private TableColumn<OnlineOrderHistoryEntry, String> historyTimeColumn;

    @FXML
    private TableColumn<OnlineOrderHistoryEntry, String> historyActionColumn;

    @FXML
    private TableColumn<OnlineOrderHistoryEntry, String> historyStatusColumn;

    @FXML
    private TableColumn<OnlineOrderHistoryEntry, String> historyNoteColumn;

    @FXML
    private Label statusLabel;

    @FXML
    private VBox detailBox;

    private final OnlineOrderPresenter presenter = new OnlineOrderPresenter(this);

    private boolean suppressSelectionLoad;

    private String currentOrderStatus;

    @FXML
    private void initialize() {
        configureTables();
        orderTable.getSelectionModel().selectedItemProperty().addListener((obs, old, value) -> {
            if (!suppressSelectionLoad) {
                presenter.onOrderSelected(value);
            }
        });
        updateWorkflowButtons(false);
        presenter.initialize();
    }

    @FXML
    public void onRefresh() {
        presenter.refresh();
    }

    @FXML
    public void onConfirm() {
        updateStatusWithNote("Confirm", OnlineOrderStatus.CONFIRMED, false);
    }

    @FXML
    public void onPreparing() {
        updateStatusWithNote("Preparing", OnlineOrderStatus.PREPARING, false);
    }

    @FXML
    public void onReady() {
        updateStatusWithNote("Ready", OnlineOrderStatus.READY, false);
    }

    @FXML
    public void onDelivering() {
        updateStatusWithNote("Delivering", OnlineOrderStatus.DELIVERING, false);
    }

    @FXML
    public void onDelivered() {
        updateStatusWithNote("Delivered", OnlineOrderStatus.DELIVERED, false);
    }

    @FXML
    public void onCompleted() {
        updateStatusWithNote("Completed", OnlineOrderStatus.COMPLETED, false);
    }

    @FXML
    public void onRefund() {
        updateStatusWithNote("Refund", OnlineOrderStatus.REFUNDED, false);
    }

    @FXML
    public void onReject() {
        updateStatusWithNote("Reject", OnlineOrderStatus.REJECTED, true);
    }

    @FXML
    public void onCancel() {
        updateStatusWithNote("Cancel", OnlineOrderStatus.CANCELLED, true);
    }

    public void setOrders(
            List<OnlineOrderSummary> orders,
            Long selectedOrderId
    ) {
        suppressSelectionLoad = true;
        orderTable.setItems(FXCollections.observableArrayList(orders));
        try {
            if (orders == null || orders.isEmpty()) {
                clearDetail();
                return;
            }
            selectOrderInTable(selectedOrderId);
        } finally {
            suppressSelectionLoad = false;
        }
    }

    public void showDetail(OnlineOrderDetail detail) {
        if (detail == null || detail.getOrder() == null) {
            clearDetail();
            return;
        }

        OnlineOrder order = detail.getOrder();
        currentOrderStatus = order.getStatus();
        orderIdValueLabel.setText(value(order.getId()));
        orderStatusValueLabel.setText(value(order.getStatus()));
        orderTotalValueLabel.setText(order.getTotalAmount() == null ? "0.00" : order.getTotalAmount().toPlainString());
        orderCreatedAtValueLabel.setText(TimeFormatUtil.formatDateTime(order.getCreatedAt()));

        customerFullNameValueLabel.setText(value(order.getCustomerFullName()));
        customerEmailValueLabel.setText(value(order.getCustomerEmail()));

        recipientNameValueLabel.setText(value(order.getRecipientName()));
        phoneValueLabel.setText(value(order.getPhone()));
        shippingAddressValueLabel.setText(value(order.getShippingAddress()));
        paymentMethodValueLabel.setText(value(order.getPaymentMethod()));

        updateWorkflowButtons(false);
        itemTable.setItems(FXCollections.observableArrayList(detail.getItems()));
        historyTable.setItems(FXCollections.observableArrayList(detail.getHistory()));
    }

    public void clearDetail() {
        orderIdValueLabel.setText("-");
        orderStatusValueLabel.setText("-");
        orderTotalValueLabel.setText("0.00");
        orderCreatedAtValueLabel.setText("-");
        customerFullNameValueLabel.setText("-");
        customerEmailValueLabel.setText("-");
        recipientNameValueLabel.setText("-");
        phoneValueLabel.setText("-");
        shippingAddressValueLabel.setText("-");
        paymentMethodValueLabel.setText("-");
        currentOrderStatus = null;
        updateWorkflowButtons(false);
        itemTable.setItems(FXCollections.observableArrayList());
        historyTable.setItems(FXCollections.observableArrayList());
        orderTable.getSelectionModel().clearSelection();
    }

    public void setBusy(boolean busy) {
        orderTable.setDisable(busy);
        itemTable.setDisable(busy);
        historyTable.setDisable(busy);
        if (refreshButton != null) {
            refreshButton.setDisable(busy);
        }
        updateWorkflowButtons(busy);
    }

    public void setStatus(String status) {
        statusLabel.setText(status == null ? "" : status);
    }

    public void showError(String message) {
        if (message == null || message.isBlank()) {
            message = "Request failed";
        }
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void configureTables() {
        orderTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        itemTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        orderIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        orderCustomerColumn.setCellValueFactory(new PropertyValueFactory<>("customerFullName"));
        orderEmailColumn.setCellValueFactory(new PropertyValueFactory<>("customerEmail"));
        orderStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        orderTotalColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        orderCreatedAtColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(TimeFormatUtil.formatDateTime(cellData.getValue().getCreatedAt())));

        itemProductColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        itemQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        itemUnitPriceColumn.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        itemSubtotalColumn.setCellValueFactory(new PropertyValueFactory<>("subtotal"));

        historyTimeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(TimeFormatUtil.formatDateTime(cellData.getValue().getCreatedAt())));
        historyActionColumn.setCellValueFactory(new PropertyValueFactory<>("action"));
        historyStatusColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        value(cellData.getValue().getOldStatus())
                                + " -> "
                                + value(cellData.getValue().getNewStatus())
                ));
        historyNoteColumn.setCellValueFactory(new PropertyValueFactory<>("note"));
    }

    private String value(Object value) {
        return value == null ? "-" : value.toString();
    }

    public Long getSelectedOrderId() {
        OnlineOrderSummary summary = orderTable.getSelectionModel().getSelectedItem();
        return summary == null ? null : summary.getId();
    }

    public void selectOrderById(Long orderId) {
        if (orderId == null) {
            return;
        }
        suppressSelectionLoad = true;
        try {
            selectOrderInTable(orderId);
        } finally {
            suppressSelectionLoad = false;
        }
    }

    private void selectOrderInTable(Long orderId) {
        if (orderId == null) {
            orderTable.getSelectionModel().selectFirst();
            return;
        }

        for (OnlineOrderSummary summary : orderTable.getItems()) {
            if (orderId.equals(summary.getId())) {
                orderTable.getSelectionModel().select(summary);
                return;
            }
        }

        orderTable.getSelectionModel().selectFirst();
    }

    private void updateStatusWithNote(
            String actionLabel,
            String nextStatus,
            boolean noteRequired
    ) {

        String note =
                requestActionNote(
                        actionLabel,
                        noteRequired
                );

        if (note == null) {
            return;
        }

        presenter.updateStatus(
                getSelectedOrderId(),
                nextStatus,
                note
        );
    }

    private String requestActionNote(
            String actionLabel,
            boolean required
    ) {

        while (true) {
            Dialog<String> dialog =
                    new Dialog<>();

            dialog.setTitle(actionLabel + " Order");
            dialog.setHeaderText(null);

            ButtonType confirmButtonType =
                    new ButtonType(
                            "Confirm",
                            ButtonBar.ButtonData.OK_DONE
                    );

            dialog.getDialogPane().getButtonTypes().setAll(
                    confirmButtonType,
                    ButtonType.CANCEL
            );

            Label noteLabel =
                    new Label(
                            required
                                    ? "Note required:"
                                    : "Note:"
                    );
            noteLabel.getStyleClass().add("analytics-card-title");

            TextArea noteArea =
                    new TextArea();
            noteArea.setPromptText(
                    required
                            ? "Enter the required action note"
                            : "Enter an optional action note"
            );
            noteArea.setWrapText(true);
            noteArea.setMinHeight(120);
            noteArea.setPrefHeight(120);
            noteArea.setPrefRowCount(5);

            VBox content =
                    new VBox(
                            10,
                            noteLabel,
                            noteArea
                    );
            content.setMinWidth(420);

            dialog.getDialogPane().setContent(content);
            dialog.setResultConverter(buttonType ->
                    buttonType == confirmButtonType
                            ? noteArea.getText()
                            : null
            );

            Optional<String> result =
                    dialog.showAndWait();

            if (result.isEmpty()) {
                return null;
            }

            String note =
                    result.get() == null
                            ? ""
                            : result.get().trim();

            if (!required || !note.isBlank()) {
                return note;
            }

            showError("A note is required for this action");
        }
    }

    private void updateWorkflowButtons(boolean busy) {
        String status = currentOrderStatus;
        boolean hasOrder = status != null && !status.isBlank();
        boolean knownStatus = OnlineOrderStatus.isKnown(status);
        boolean canModify =
                PermissionGuard.canModifyOnlineOrders();
        if (confirmButton != null) {
            confirmButton.setDisable(!canModify || busy || !hasOrder || !knownStatus || !OnlineOrderStatus.PENDING.equals(status));
        }
        if (preparingButton != null) {
            preparingButton.setDisable(!canModify || busy || !hasOrder || !knownStatus || !OnlineOrderStatus.CONFIRMED.equals(status));
        }
        if (readyButton != null) {
            readyButton.setDisable(!canModify || busy || !hasOrder || !knownStatus || !OnlineOrderStatus.PREPARING.equals(status));
        }
        if (deliveringButton != null) {
            deliveringButton.setDisable(!canModify || busy || !hasOrder || !knownStatus || !OnlineOrderStatus.READY.equals(status));
        }
        if (deliveredButton != null) {
            deliveredButton.setDisable(!canModify || busy || !hasOrder || !knownStatus || !OnlineOrderStatus.DELIVERING.equals(status));
        }
        if (completedButton != null) {
            completedButton.setDisable(!canModify || busy || !hasOrder || !knownStatus || !OnlineOrderStatus.DELIVERED.equals(status));
        }
        if (refundButton != null) {
            refundButton.setDisable(!PermissionGuard.canRefundOnlineOrder() || busy || !hasOrder || !knownStatus
                    || (!OnlineOrderStatus.DELIVERED.equals(status)
                    && !OnlineOrderStatus.COMPLETED.equals(status)));
        }
        if (rejectButton != null) {
            rejectButton.setDisable(!canModify || busy || !hasOrder || !knownStatus
                    || OnlineOrderStatus.DELIVERING.equals(status)
                    || OnlineOrderStatus.DELIVERED.equals(status)
                    || OnlineOrderStatus.COMPLETED.equals(status)
                    || OnlineOrderStatus.CANCELLED.equals(status)
                    || OnlineOrderStatus.REFUNDED.equals(status)
                    || OnlineOrderStatus.REJECTED.equals(status));
        }
        if (cancelButton != null) {
            cancelButton.setDisable(!PermissionGuard.canCancelOnlineOrder() || busy || !hasOrder || !knownStatus
                    || OnlineOrderStatus.DELIVERING.equals(status)
                    || OnlineOrderStatus.DELIVERED.equals(status)
                    || OnlineOrderStatus.COMPLETED.equals(status)
                    || OnlineOrderStatus.CANCELLED.equals(status)
                    || OnlineOrderStatus.REFUNDED.equals(status)
                    || OnlineOrderStatus.REJECTED.equals(status));
        }
    }
}
