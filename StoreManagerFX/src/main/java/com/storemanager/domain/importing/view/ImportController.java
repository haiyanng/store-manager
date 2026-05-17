package com.storemanager.domain.importing.view;

import com.storemanager.domain.importing.model.ImportCartItem;
import com.storemanager.domain.importing.model.ImportReceipt;
import com.storemanager.domain.importing.presenter.ImportPresenter;
import com.storemanager.domain.product.model.Product;
import com.storemanager.domain.product.view.ProductLookupComboBoxSupport;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ImportController {

    @FXML
    private TextField supplierField;

    @FXML
    private ComboBox<Product> productComboBox;

    @FXML
    private TextField quantityField;

    @FXML
    private TextField unitCostField;

    @FXML
    private TableView<ImportCartItem> importItemTable;

    @FXML
    private TableColumn<ImportCartItem, String> itemProductColumn;

    @FXML
    private TableColumn<ImportCartItem, String> itemSkuColumn;

    @FXML
    private TableColumn<ImportCartItem, Integer> itemQuantityColumn;

    @FXML
    private TableColumn<ImportCartItem, BigDecimal> itemUnitCostColumn;

    @FXML
    private TableColumn<ImportCartItem, BigDecimal> itemSubtotalColumn;

    @FXML
    private TableView<ImportReceipt> receiptTable;

    @FXML
    private TableColumn<ImportReceipt, Long> receiptIdColumn;

    @FXML
    private TableColumn<ImportReceipt, String> receiptSupplierColumn;

    @FXML
    private TableColumn<ImportReceipt, BigDecimal> receiptTotalColumn;

    @FXML
    private TableColumn<ImportReceipt, Long> receiptUserColumn;

    @FXML
    private TableColumn<ImportReceipt, LocalDateTime> receiptCreatedAtColumn;

    @FXML
    private Label totalLabel;

    @FXML
    private Label statusLabel;

    private ProductLookupComboBoxSupport productLookupSupport;

    private ImportPresenter presenter;

    @FXML
    public void initialize() {

        presenter =
                new ImportPresenter(
                        this
                );

        productLookupSupport =
                new ProductLookupComboBoxSupport(productComboBox);
        configureImportItemTable();
        configureReceiptTable();

        presenter.initialize();
    }

    @FXML
    public void onAddItem() {

        Product selectedProduct =
                productLookupSupport.resolveSelectionFromEditor();

        presenter.addItem(
                selectedProduct,
                parseQuantity(),
                parseUnitCost()
        );
    }

    @FXML
    public void onRemoveItem() {

        presenter.removeItem(
                importItemTable
                        .getSelectionModel()
                        .getSelectedItem()
        );
    }

    @FXML
    public void onClearItems() {

        presenter.clearItems();
    }

    @FXML
    public void onFinalizeImport() {

        presenter.finalizeImport(
                supplierField.getText()
        );
    }

    @FXML
    public void onRefresh() {

        presenter.loadImportData();
    }

    public void setProducts(
            List<Product> products
    ) {

        productLookupSupport.setProducts(products);
    }

    public void setImportItems(
            List<ImportCartItem> items
    ) {

        importItemTable.setItems(
                FXCollections.observableArrayList(
                        items
                )
        );
    }

    public void setRecentReceipts(
            List<ImportReceipt> receipts
    ) {

        receiptTable.setItems(
                FXCollections.observableArrayList(
                        receipts
                )
        );
    }

    public void setTotalCost(
            BigDecimal totalCost
    ) {

        totalLabel.setText(
                totalCost == null
                        ? "0.00"
                        : totalCost.toPlainString()
        );
    }

    public void clearEntryForm() {

        productLookupSupport.clearSelection();
        quantityField.clear();
        unitCostField.clear();
        importItemTable.getSelectionModel().clearSelection();
    }

    public void clearSupplier() {

        supplierField.clear();
    }

    public void setBusy(
            boolean busy
    ) {

        supplierField.setDisable(busy);
        productComboBox.setDisable(busy);
        quantityField.setDisable(busy);
        unitCostField.setDisable(busy);
        importItemTable.setDisable(busy);
        receiptTable.setDisable(busy);
    }

    public void setStatus(
            String status
    ) {

        statusLabel.setText(status);
    }

    public void showError(
            String message
    ) {

        Alert alert =
                new Alert(
                        Alert.AlertType.ERROR
                );

        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void configureImportItemTable() {

        itemProductColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                cellData.getValue().getProduct().getName()
                        )
        );

        itemSkuColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                cellData.getValue().getProduct().getSku()
                        )
        );

        itemQuantityColumn.setCellValueFactory(
                new PropertyValueFactory<>("quantity")
        );

        itemUnitCostColumn.setCellValueFactory(
                cellData ->
                        new SimpleObjectProperty<>(
                                cellData.getValue().getUnitCost()
                        )
        );

        itemSubtotalColumn.setCellValueFactory(
                cellData ->
                        new SimpleObjectProperty<>(
                                cellData.getValue().getSubtotal()
                        )
        );
    }

    private void configureReceiptTable() {

        receiptIdColumn.setCellValueFactory(
                new PropertyValueFactory<>("id")
        );

        receiptSupplierColumn.setCellValueFactory(
                new PropertyValueFactory<>("supplierName")
        );

        receiptTotalColumn.setCellValueFactory(
                new PropertyValueFactory<>("totalCost")
        );

        receiptUserColumn.setCellValueFactory(
                new PropertyValueFactory<>("createdByUserId")
        );

        receiptCreatedAtColumn.setCellValueFactory(
                new PropertyValueFactory<>("createdAt")
        );
    }

    private int parseQuantity() {

        String value =
                quantityField.getText();

        if (value == null || value.trim().isEmpty()) {
            return 0;
        }

        return Integer.parseInt(
                value.trim()
        );
    }

    private BigDecimal parseUnitCost() {

        String value =
                unitCostField.getText();

        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        return new BigDecimal(
                value.trim()
        );
    }
}
