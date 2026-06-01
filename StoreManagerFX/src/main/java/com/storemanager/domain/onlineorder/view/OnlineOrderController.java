package com.storemanager.domain.onlineorder.view;

import com.storemanager.core.util.TimeFormatUtil;
import com.storemanager.domain.onlineorder.model.OnlineOrder;
import com.storemanager.domain.onlineorder.model.OnlineOrderDetail;
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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import java.math.BigDecimal;
import java.util.List;

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
    private Button deliveringButton;

    @FXML
    private Button deliveredButton;

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
        presenter.initialize();
    }

    @FXML
    public void onRefresh() {
        presenter.refresh();
    }

    @FXML
    public void onConfirm() {
        presenter.updateStatus(selectedOrderId(), OnlineOrderStatus.CONFIRMED);
    }

    @FXML
    public void onPreparing() {
        presenter.updateStatus(selectedOrderId(), OnlineOrderStatus.PREPARING);
    }

    @FXML
    public void onDelivering() {
        presenter.updateStatus(selectedOrderId(), OnlineOrderStatus.DELIVERING);
    }

    @FXML
    public void onDelivered() {
        presenter.updateStatus(selectedOrderId(), OnlineOrderStatus.DELIVERED);
    }

    @FXML
    public void onCancel() {
        presenter.updateStatus(selectedOrderId(), OnlineOrderStatus.CANCELLED);
    }

    public void setOrders(List<OnlineOrderSummary> orders) {
        suppressSelectionLoad = true;
        orderTable.setItems(FXCollections.observableArrayList(orders));
        try {
            if (orders == null || orders.isEmpty()) {
                clearDetail();
                return;
            }
            orderTable.getSelectionModel().selectFirst();
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
        orderTable.getSelectionModel().clearSelection();
    }

    public void setBusy(boolean busy) {
        orderTable.setDisable(busy);
        itemTable.setDisable(busy);
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
    }

    private String value(Object value) {
        return value == null ? "-" : value.toString();
    }

    private Long selectedOrderId() {
        OnlineOrderSummary summary = orderTable.getSelectionModel().getSelectedItem();
        return summary == null ? null : summary.getId();
    }

    public void selectOrderById(Long orderId) {
        if (orderId == null) {
            return;
        }
        for (OnlineOrderSummary summary : orderTable.getItems()) {
            if (orderId.equals(summary.getId())) {
                suppressSelectionLoad = true;
                try {
                    orderTable.getSelectionModel().select(summary);
                } finally {
                    suppressSelectionLoad = false;
                }
                return;
            }
        }
    }

    private void updateWorkflowButtons(boolean busy) {
        String status = currentOrderStatus;
        boolean hasOrder = status != null && !status.isBlank();
        boolean knownStatus = OnlineOrderStatus.isKnown(status);
        if (confirmButton != null) {
            confirmButton.setDisable(busy || !hasOrder || !knownStatus || !OnlineOrderStatus.PENDING.equals(status));
        }
        if (preparingButton != null) {
            preparingButton.setDisable(busy || !hasOrder || !knownStatus || !OnlineOrderStatus.CONFIRMED.equals(status));
        }
        if (deliveringButton != null) {
            deliveringButton.setDisable(busy || !hasOrder || !knownStatus || !OnlineOrderStatus.PREPARING.equals(status));
        }
        if (deliveredButton != null) {
            deliveredButton.setDisable(busy || !hasOrder || !knownStatus || !OnlineOrderStatus.DELIVERING.equals(status));
        }
        if (cancelButton != null) {
            cancelButton.setDisable(busy || !hasOrder || !knownStatus || OnlineOrderStatus.DELIVERED.equals(status) || OnlineOrderStatus.CANCELLED.equals(status));
        }
    }
}
