package com.storemanager.domain.inventory.view;

import com.storemanager.domain.inventory.model.InventoryTransaction;
import com.storemanager.domain.inventory.model.InventoryTransactionType;
import com.storemanager.domain.product.model.Product;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

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

        productComboBox.setConverter(
                new StringConverter<>() {
                    @Override
                    public String toString(
                            Product product
                    ) {

                        if (product == null) {
                            return "";
                        }

                        return product.getName()
                                + " / "
                                + product.getSku();
                    }

                    @Override
                    public Product fromString(
                            String value
                    ) {

                        return null;
                    }
                }
        );
    }

    public InventoryTransaction readTransaction() {

        InventoryTransaction transaction =
                new InventoryTransaction();

        Product product =
                productComboBox.getValue();

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

        productComboBox.setItems(
                FXCollections.observableArrayList(
                        products
                )
        );
    }

    public void clear() {

        productComboBox.setValue(null);
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
