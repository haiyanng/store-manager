package com.storemanager.domain.inventory.view;

import com.storemanager.domain.inventory.model.InventoryTransaction;
import com.storemanager.domain.inventory.model.InventoryTransactionType;
import com.storemanager.domain.product.model.Product;
import com.storemanager.domain.product.view.ProductLookupComboBoxSupport;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.util.List;

public class InventoryAdjustmentFormController {

    @FXML
    private ComboBox<Product> productComboBox;

    @FXML
    private ComboBox<InventoryTransactionType> typeComboBox;

    @FXML
    private TextField quantityField;

    @FXML
    private TextField reasonField;

    private ProductLookupComboBoxSupport productLookupSupport;

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

        productLookupSupport =
                new ProductLookupComboBoxSupport(productComboBox);
    }

    public InventoryTransaction readTransaction() {

        InventoryTransaction transaction =
                new InventoryTransaction();

        Product product =
                productLookupSupport.resolveSelectionFromEditor();

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

        productLookupSupport.setProducts(products);
    }

    public void clear() {

        productLookupSupport.clearSelection();
        typeComboBox.setValue(
                InventoryTransactionType.IMPORT
        );
        quantityField.clear();
        reasonField.clear();
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
