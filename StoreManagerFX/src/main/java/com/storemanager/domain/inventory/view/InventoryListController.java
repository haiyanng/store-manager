package com.storemanager.domain.inventory.view;

import com.storemanager.core.util.UiFeedback;
import com.storemanager.domain.inventory.model.InventoryItem;
import com.storemanager.domain.inventory.model.InventoryTransaction;
import com.storemanager.domain.inventory.presenter.InventoryPresenter;
import com.storemanager.core.util.TimeFormatUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class InventoryListController {

    @FXML
    private TableView<InventoryItem> inventoryTable;

    @FXML
    private TableColumn<InventoryItem, Long> itemIdColumn;

    @FXML
    private TableColumn<InventoryItem, String> itemProductColumn;

    @FXML
    private TableColumn<InventoryItem, String> itemSkuColumn;

    @FXML
    private TableColumn<InventoryItem, Integer> itemQuantityColumn;

    @FXML
    private TableColumn<InventoryItem, String> itemUpdatedAtColumn;

    @FXML private TableColumn<InventoryItem, String> itemExpiryColumn;
    @FXML private TableColumn<InventoryItem, String> itemExpiryStatusColumn;

    @FXML
    private TableView<InventoryTransaction> transactionTable;

    @FXML
    private TableColumn<InventoryTransaction, Long> transactionIdColumn;

    @FXML
    private TableColumn<InventoryTransaction, String> transactionProductColumn;

    @FXML
    private TableColumn<InventoryTransaction, String> transactionTypeColumn;

    @FXML
    private TableColumn<InventoryTransaction, Integer> transactionQuantityColumn;

    @FXML
    private TableColumn<InventoryTransaction, String> transactionReasonColumn;

    @FXML
    private TableColumn<InventoryTransaction, Long> transactionUserColumn;

    @FXML
    private TableColumn<InventoryTransaction, String> transactionCreatedAtColumn;

    @FXML
    private Label statusLabel;

    private InventoryPresenter presenter;

    @FXML
    public void initialize() {
        UiFeedback.emptyTable(inventoryTable, "No stock records found.");
        UiFeedback.emptyTable(transactionTable, "No inventory transactions found.");


        presenter =
                new InventoryPresenter(
                        this
                );

        configureInventoryTable();
        configureTransactionTable();

        presenter.initialize();
    }

    @FXML
    public void onRefresh() {

        presenter.refresh();
    }

    public void setInventoryItems(
            List<InventoryItem> items
    ) {

        inventoryTable.setItems(
                FXCollections.observableArrayList(
                        items
                )
        );
    }

    public void setTransactions(
            List<InventoryTransaction> transactions
    ) {

        transactionTable.setItems(
                FXCollections.observableArrayList(
                        transactions
                )
        );
    }

    public void setBusy(
            boolean busy
    ) {

        inventoryTable.setDisable(busy);
        transactionTable.setDisable(busy);
    }

    public void setStatus(
            String status
    ) {

        UiFeedback.status(statusLabel, status, inventoryTable, transactionTable);
    }

    public void showError(
            String message
    ) {
        UiFeedback.showError(message);
    }

    private void configureInventoryTable() {
        itemExpiryColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getNearestExpiryDate() == null ? "N/A"
                        : cell.getValue().getNearestExpiryDate().toString()));
        itemExpiryStatusColumn.setCellValueFactory(new PropertyValueFactory<>("expiryStatus"));

        itemIdColumn.setCellValueFactory(
                new PropertyValueFactory<>("id")
        );

        itemProductColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                presenter.getProductName(
                                        cellData.getValue().getProductId()
                                )
                        )
        );

        itemSkuColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                presenter.getProductSku(
                                        cellData.getValue().getProductId()
                                )
                        )
        );

        itemQuantityColumn.setCellValueFactory(
                new PropertyValueFactory<>("quantity")
        );

        itemUpdatedAtColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        TimeFormatUtil.formatDateTime(
                                cellData.getValue().getUpdatedAt()
                        )
                )
        );
    }

    private void configureTransactionTable() {

        transactionIdColumn.setCellValueFactory(
                new PropertyValueFactory<>("id")
        );

        transactionProductColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                presenter.getProductName(
                                        cellData.getValue().getProductId()
                                )
                        )
        );

        transactionTypeColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                cellData.getValue().getType() == null
                                        ? ""
                                        : cellData.getValue().getType().name()
                        )
        );

        transactionQuantityColumn.setCellValueFactory(
                new PropertyValueFactory<>("quantity")
        );

        transactionReasonColumn.setCellValueFactory(
                new PropertyValueFactory<>("reason")
        );

        transactionUserColumn.setCellValueFactory(
                new PropertyValueFactory<>("createdByUserId")
        );

        transactionCreatedAtColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        TimeFormatUtil.formatDateTime(
                                cellData.getValue().getCreatedAt()
                        )
                )
        );
    }
}
