package com.storemanager.domain.inventory.view;

import com.storemanager.domain.inventory.model.InventoryTransaction;
import com.storemanager.domain.inventory.model.InventoryTransactionType;
import com.storemanager.domain.product.model.Product;
import com.storemanager.domain.product.view.ProductSelectionWorkflowController;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.util.List;

public class InventoryAdjustmentFormController {

    @FXML
    private ProductSelectionWorkflowController productSelectionWorkflowController;

    @FXML
    private ComboBox<InventoryTransactionType> typeComboBox;

    @FXML
    private TextField quantityField;

    @FXML
    private TextField reasonField;

    @FXML
    public void initialize() {

        typeComboBox.setItems(
                FXCollections.observableArrayList(
                        InventoryTransactionType.values()
                )
        );

        typeComboBox.setValue(
                InventoryTransactionType.IMPORT
        );

    }

    public InventoryTransaction readTransaction() {

        InventoryTransaction transaction =
                new InventoryTransaction();

        Product product =
                productSelectionWorkflowController.resolveSelectedProduct();

        transaction.setProductId(
                product == null
                        ? null
                        : product.getId()
        );

        transaction.setType(
                typeComboBox.getValue()
        );

        transaction.setQuantity(
                parseQuantity()
        );

        transaction.setReason(
                reasonField.getText()
        );

        return transaction;
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

    public void clear() {

        productSelectionWorkflowController.clearSelection();
        typeComboBox.setValue(
                InventoryTransactionType.IMPORT
        );
        quantityField.clear();
        reasonField.clear();
    }

    public void setBusy(
            boolean busy
    ) {

        productSelectionWorkflowController.setBusy(busy);
        typeComboBox.setDisable(busy);
        quantityField.setDisable(busy);
        reasonField.setDisable(busy);
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
