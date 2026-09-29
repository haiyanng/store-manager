package com.storemanager.domain.sale.view;

import com.storemanager.core.util.UiFeedback;
import com.storemanager.core.storage.ImageStorageService;
import com.storemanager.domain.product.model.Product;
import com.storemanager.domain.product.view.ProductSelectionWorkflowController;
import com.storemanager.domain.sale.model.SaleCartItem;
import com.storemanager.domain.sale.model.SaleOrder;
import com.storemanager.domain.sale.model.SaleOrderItemDetail;
import com.storemanager.domain.sale.model.SelectedProductPreviewDto;
import com.storemanager.domain.sale.presenter.SalePresenter;
import com.storemanager.core.util.TimeFormatUtil;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;

public class SaleController {

    private final ImageStorageService imageStorageService =
            new ImageStorageService();

    @FXML
    private ProductSelectionWorkflowController productSelectionWorkflowController;

    @FXML
    private javafx.scene.control.TextField quantityField;

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
    private TableColumn<SaleOrder, String> orderCreatedAtColumn;

    @FXML
    private TableView<SaleOrderItemDetail> orderDetailTable;

    @FXML
    private TableColumn<SaleOrderItemDetail, String> detailProductColumn;

    @FXML
    private TableColumn<SaleOrderItemDetail, String> detailSkuColumn;

    @FXML
    private TableColumn<SaleOrderItemDetail, Integer> detailQuantityColumn;

    @FXML
    private TableColumn<SaleOrderItemDetail, BigDecimal> detailUnitPriceColumn;

    @FXML
    private TableColumn<SaleOrderItemDetail, BigDecimal> detailSubtotalColumn;

    @FXML
    private Label totalLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Label selectedProductPreviewMessageLabel;

    @FXML
    private Label selectedProductPreviewNameLabel;

    @FXML
    private Label selectedProductPreviewSkuLabel;

    @FXML
    private Label selectedProductPreviewBarcodeLabel;

    @FXML
    private Label selectedProductPreviewPriceLabel;

    @FXML
    private Label selectedProductPreviewStockLabel;

    @FXML
    private Label selectedProductImagePlaceholderLabel;

    @FXML
    private ImageView selectedProductImageView;

    private SalePresenter presenter;

    @FXML
    public void initialize() {
        UiFeedback.emptyTable(cartTable, "Your order is empty. Select a product and choose Add item.");
        UiFeedback.emptyTable(orderTable, "No orders found.");
        UiFeedback.emptyTable(orderDetailTable, "Select an order to view its items.");
        UiFeedback.moneyColumn(cartUnitPriceColumn);
        UiFeedback.moneyColumn(cartSubtotalColumn);
        UiFeedback.moneyColumn(orderTotalColumn);
        UiFeedback.moneyColumn(detailUnitPriceColumn);
        UiFeedback.moneyColumn(detailSubtotalColumn);


        presenter =
                new SalePresenter(
                        this
                );

        productSelectionWorkflowController.setSelectedProductListener(
                product ->
                        presenter.onSelectedProductChanged(product)
        );

        configureCartTable();
        configureOrderTable();
        configureOrderDetailTable();
        clearSelectedProductPreview();

        presenter.initialize();
    }

    @FXML
    public void onAddToCart() {

        try {

            Product selectedProduct =
                    productSelectionWorkflowController.resolveSelectedProduct();

            presenter.addProductToCart(
                    selectedProduct,
                    parseQuantity()
            );

        } catch (NumberFormatException e) {

            showError("Quantity must be a valid number");
        }
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

        productSelectionWorkflowController.setProducts(products);
    }

    public void setQuickPickProducts(
            List<Product> products
    ) {

        productSelectionWorkflowController.setQuickPickProducts(products);
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

    public void setOrderItemDetails(
            List<SaleOrderItemDetail> details
    ) {

        orderDetailTable.setItems(
                FXCollections.observableArrayList(
                        details == null ? List.of() : details
                )
        );
    }

    public void setTotalAmount(
            BigDecimal totalAmount
    ) {

        totalLabel.setText(
                UiFeedback.money(totalAmount)
        );
    }

    public void clearEntryForm() {

        productSelectionWorkflowController.clearSelection();
        quantityField.clear();
        cartTable.getSelectionModel().clearSelection();
    }

    public void showSelectedProductPreview(
            SelectedProductPreviewDto preview
    ) {

        if (preview == null) {
            clearSelectedProductPreview();
            return;
        }

        selectedProductPreviewMessageLabel.setText("Selected product");
        selectedProductPreviewNameLabel.setText(
                "Name: " + safeText(preview.name())
        );
        selectedProductPreviewSkuLabel.setText(
                "SKU: " + safeText(preview.sku())
        );
        selectedProductPreviewBarcodeLabel.setText(
                "Barcode: " + safeText(preview.barcode())
        );
        selectedProductPreviewPriceLabel.setText(
                "Unit price: " + formatPrice(preview.unitPrice())
        );
        selectedProductPreviewStockLabel.setText(
                "Stock: "
                        + (preview.stockQuantity() == null
                        ? "N/A"
                        : preview.stockQuantity())
        );

        showProductImage(preview.imagePath());
    }

    public void clearSelectedProductPreview() {

        selectedProductPreviewMessageLabel.setText(
                "Select a product to preview"
        );
        selectedProductPreviewNameLabel.setText("Name: -");
        selectedProductPreviewSkuLabel.setText("SKU: -");
        selectedProductPreviewBarcodeLabel.setText("Barcode: -");
        selectedProductPreviewPriceLabel.setText("Unit price: 0.00");
        selectedProductPreviewStockLabel.setText("Stock: N/A");

        selectedProductImageView.setImage(null);
        selectedProductImagePlaceholderLabel.setVisible(false);
        selectedProductImagePlaceholderLabel.setManaged(false);
    }

    public void setBusy(
            boolean busy
    ) {

        productSelectionWorkflowController.setBusy(busy);
        quantityField.setDisable(busy);
        cartTable.setDisable(busy);
        orderTable.setDisable(busy);
        orderDetailTable.setDisable(busy);
    }

    public void setStatus(
            String status
    ) {

        if (status.toLowerCase().contains("details")) {
            UiFeedback.status(statusLabel, status, orderDetailTable);
        } else {
            UiFeedback.status(statusLabel, status, orderTable);
        }
    }

    public void showError(
            String message
    ) {
        UiFeedback.showError(message);
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
                cellData -> new SimpleStringProperty(
                        TimeFormatUtil.formatDateTime(
                                cellData.getValue().getCreatedAt()
                        )
                )
        );

        orderTable
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (observable, oldValue, newValue) ->
                                presenter.selectOrder(newValue)
                );
    }

    private void configureOrderDetailTable() {

        detailProductColumn.setCellValueFactory(
                new PropertyValueFactory<>("productName")
        );

        detailSkuColumn.setCellValueFactory(
                new PropertyValueFactory<>("sku")
        );

        detailQuantityColumn.setCellValueFactory(
                new PropertyValueFactory<>("quantity")
        );

        detailUnitPriceColumn.setCellValueFactory(
                new PropertyValueFactory<>("unitPrice")
        );

        detailSubtotalColumn.setCellValueFactory(
                new PropertyValueFactory<>("subtotal")
        );
    }

    private void showProductImage(
            String imagePath
    ) {

        selectedProductImageView.setImage(null);
        selectedProductImagePlaceholderLabel.setText("No image");
        selectedProductImagePlaceholderLabel.setVisible(true);
        selectedProductImagePlaceholderLabel.setManaged(true);

        File imageFile =
                imageStorageService.resolveImageFile(imagePath);

        if (imageFile == null || !imageFile.isFile()) {
            if (imagePath != null && !imagePath.trim().isEmpty()) {
                System.err.println(
                        "[SALE_PREVIEW] Missing product image: "
                                + imagePath
                );
            }
            return;
        }

        try {

            Image image =
                    new Image(
                            imageFile.toURI().toString(),
                            140,
                            140,
                            true,
                            true,
                            false
                    );

            if (image.isError()) {
                System.err.println(
                        "[SALE_PREVIEW] Unable to load product image: "
                                + imagePath
                );
                return;
            }

            selectedProductImageView.setImage(image);
            selectedProductImagePlaceholderLabel.setVisible(false);
            selectedProductImagePlaceholderLabel.setManaged(false);

        } catch (Exception e) {

            System.err.println(
                    "[SALE_PREVIEW] Unable to load product image: "
                            + imagePath
            );
        }
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

    private String safeText(
            String value
    ) {

        if (value == null || value.trim().isEmpty()) {
            return "-";
        }

        return value.trim();
    }

    private String formatPrice(
            BigDecimal value
    ) {

        return UiFeedback.money(value);
    }
}
