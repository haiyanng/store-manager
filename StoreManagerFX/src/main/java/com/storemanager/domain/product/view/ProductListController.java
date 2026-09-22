package com.storemanager.domain.product.view;

import com.storemanager.domain.category.model.Category;
import com.storemanager.core.security.PermissionGuard;
import com.storemanager.domain.product.model.Product;
import com.storemanager.domain.product.presenter.ProductPresenter;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.util.List;

public class ProductListController {

    @FXML
    private ProductFormController productFormController;

    @FXML
    private TableView<Product> productTable;

    @FXML
    private TableColumn<Product, Long> idColumn;

    @FXML
    private TableColumn<Product, String> nameColumn;

    @FXML
    private TableColumn<Product, String> skuColumn;

    @FXML
    private TableColumn<Product, String> barcodeColumn;

    @FXML
    private TableColumn<Product, String> categoryColumn;

    @FXML
    private TableColumn<Product, BigDecimal> basePriceColumn;

    @FXML
    private TableColumn<Product, String> unitColumn;

    @FXML
    private TableColumn<Product, Boolean> activeColumn;

    @FXML
    private Button createButton;

    @FXML
    private Button updateButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Label statusLabel;

    private ProductPresenter presenter;

    @FXML
    public void initialize() {

        presenter =
                new ProductPresenter(
                        this
                );

        productTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );

        idColumn.setCellValueFactory(
                new PropertyValueFactory<>("id")
        );

        nameColumn.setCellValueFactory(
                new PropertyValueFactory<>("name")
        );

        skuColumn.setCellValueFactory(
                new PropertyValueFactory<>("sku")
        );

        barcodeColumn.setCellValueFactory(
                new PropertyValueFactory<>("barcode")
        );

        categoryColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                presenter.getCategoryName(
                                        cellData.getValue()
                                )
                        )
        );

        basePriceColumn.setCellValueFactory(
                new PropertyValueFactory<>("basePrice")
        );

        unitColumn.setCellValueFactory(
                new PropertyValueFactory<>("unit")
        );

        activeColumn.setCellValueFactory(
                new PropertyValueFactory<>("active")
        );

        productTable
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (observable, oldValue, newValue) ->
                                presenter.selectProduct(newValue)
                );

        presenter.initialize();
    }

    @FXML
    public void onCreate() {

        presenter.saveProduct(
                productFormController.readProduct()
        );
    }

    @FXML
    public void onUpdate() {

        presenter.saveProduct(
                productFormController.readProduct()
        );
    }

    @FXML
    public void onDelete() {

        presenter.deleteProduct();
    }

    @FXML
    public void onClear() {

        presenter.clearForm();
    }

    @FXML
    public void onRefresh() {

        presenter.refreshTable();
    }

    public void setProducts(
            List<Product> products
    ) {

        productTable.setItems(
                FXCollections.observableArrayList(
                        products
                )
        );
    }

    public void setCategories(
            List<Category> categories
    ) {

        productFormController.setCategories(
                categories
        );
    }

    public void showProduct(
            Product product
    ) {

        productFormController.showProduct(
                product
        );
    }

    public void clearSelection() {

        productTable.getSelectionModel().clearSelection();
    }

    public void clearProductForm() {

        productFormController.clear();
    }

    public void setUpdateEnabled(
            boolean enabled
    ) {

        updateButton.setDisable(
                !enabled || !PermissionGuard.canModifyProduct()
        );
    }

    public void setDeleteEnabled(
            boolean enabled
    ) {

        deleteButton.setDisable(
                !enabled || !PermissionGuard.canModifyProduct()
        );
    }

    public void setBusy(
            boolean busy
    ) {

        productTable.setDisable(busy);
        boolean canModify =
                PermissionGuard.canModifyProduct();
        createButton.setDisable(busy || !canModify);
        productFormController.setBusy(busy || !canModify);
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
}
