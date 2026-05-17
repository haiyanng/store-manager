package com.storemanager.domain.sale.view;

import com.storemanager.domain.product.model.Product;
import com.storemanager.domain.product.view.ProductLookupComboBoxSupport;
import com.storemanager.domain.sale.model.SaleCartItem;
import com.storemanager.domain.sale.model.SaleOrder;
import com.storemanager.domain.sale.presenter.SalePresenter;
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

public class SaleController {

    @FXML
    private ComboBox<Product> productComboBox;

    @FXML
    private TextField quantityField;

    @FXML
    private TableView<SaleCartItem> cartTable;

    @FXML
    private TableColumn<SaleCartItem, String> cartProductColumn;

    @FXML
    private TableColumn<SaleCartItem, String> cartSkuColumn;

    @FXML
    private TableColumn<SaleCartItem, Integer> cartQuantityColumn;

    @FXML
    private TableColumn<SaleCartItem, BigDecimal> cartUnitPriceColumn;

    @FXML
    private TableColumn<SaleCartItem, BigDecimal> cartSubtotalColumn;

    @FXML
    private TableView<SaleOrder> orderTable;

    @FXML
    private TableColumn<SaleOrder, Long> orderIdColumn;

    @FXML
    private TableColumn<SaleOrder, Long> orderUserColumn;

    @FXML
    private TableColumn<SaleOrder, BigDecimal> orderTotalColumn;

    @FXML
    private TableColumn<SaleOrder, LocalDateTime> orderCreatedAtColumn;

    @FXML
    private Label totalLabel;

    @FXML
    private Label statusLabel;

    private ProductLookupComboBoxSupport productLookupSupport;

    private SalePresenter presenter;

    @FXML
    public void initialize() {

        presenter =
                new SalePresenter(
                        this
                );

        productLookupSupport =
                new ProductLookupComboBoxSupport(productComboBox);
        configureCartTable();
        configureOrderTable();

        presenter.initialize();
    }

    @FXML
    public void onAddToCart() {

        Product selectedProduct =
                productLookupSupport.resolveSelectionFromEditor();

        presenter.addProductToCart(
                selectedProduct,
                parseQuantity()
        );
    }

    @FXML
    public void onRemoveItem() {

        presenter.removeCartItem(
                cartTable
                        .getSelectionModel()
                        .getSelectedItem()
        );
    }

    @FXML
    public void onClearCart() {

        presenter.clearCart();
    }

    @FXML
    public void onFinalizeSale() {

        presenter.finalizeSale();
    }

    @FXML
    public void onRefresh() {

        presenter.loadSaleData();
    }

    public void setProducts(
            List<Product> products
    ) {

        productLookupSupport.setProducts(products);
    }

    public void setCartItems(
            List<SaleCartItem> cartItems
    ) {

        cartTable.setItems(
                FXCollections.observableArrayList(
                        cartItems
                )
        );
    }

    public void setRecentOrders(
            List<SaleOrder> orders
    ) {

        orderTable.setItems(
                FXCollections.observableArrayList(
                        orders
                )
        );
    }

    public void setTotalAmount(
            BigDecimal totalAmount
    ) {

        totalLabel.setText(
                totalAmount == null
                        ? "0.00"
                        : totalAmount.toPlainString()
        );
    }

    public void clearEntryForm() {

        productLookupSupport.clearSelection();
        quantityField.clear();
        cartTable.getSelectionModel().clearSelection();
    }

    public void setBusy(
            boolean busy
    ) {

        productComboBox.setDisable(busy);
        quantityField.setDisable(busy);
        cartTable.setDisable(busy);
        orderTable.setDisable(busy);
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

    private void configureCartTable() {

        cartProductColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                cellData.getValue().getProduct().getName()
                        )
        );

        cartSkuColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                cellData.getValue().getProduct().getSku()
                        )
        );

        cartQuantityColumn.setCellValueFactory(
                new PropertyValueFactory<>("quantity")
        );

        cartUnitPriceColumn.setCellValueFactory(
                cellData ->
                        new SimpleObjectProperty<>(
                                cellData.getValue().getUnitPrice()
                        )
        );

        cartSubtotalColumn.setCellValueFactory(
                cellData ->
                        new SimpleObjectProperty<>(
                                cellData.getValue().getSubtotal()
                        )
        );
    }

    private void configureOrderTable() {

        orderIdColumn.setCellValueFactory(
                new PropertyValueFactory<>("id")
        );

        orderUserColumn.setCellValueFactory(
                new PropertyValueFactory<>("createdByUserId")
        );

        orderTotalColumn.setCellValueFactory(
                new PropertyValueFactory<>("totalAmount")
        );

        orderCreatedAtColumn.setCellValueFactory(
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
}
