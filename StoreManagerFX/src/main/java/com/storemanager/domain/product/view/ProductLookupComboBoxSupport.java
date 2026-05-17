package com.storemanager.domain.product.view;

import com.storemanager.domain.product.model.Product;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ProductLookupComboBoxSupport {

    private final ComboBox<Product> comboBox;

    private final ObservableList<Product> masterProducts =
            FXCollections.observableArrayList();

    private boolean updatingEditor;

    public ProductLookupComboBoxSupport(
            ComboBox<Product> comboBox
    ) {

        this.comboBox = comboBox;
        configure();
    }

    public void setProducts(
            List<Product> products
    ) {

        masterProducts.setAll(
                products == null
                        ? List.of()
                        : products
        );

        updateFilteredItems(
                comboBox.getEditor() == null
                        ? ""
                        : comboBox.getEditor().getText(),
                false
        );
    }

    public void clearSelection() {

        comboBox.setValue(null);
        if (comboBox.getEditor() != null) {
            comboBox.getEditor().clear();
        }
        updateFilteredItems("", false);
    }

    public Product resolveSelectionFromEditor() {

        String query =
                comboBox.getEditor() == null
                        ? null
                        : comboBox.getEditor().getText();

        Product matched =
                findBestMatch(query);

        if (matched != null) {
            comboBox.setValue(matched);
        } else {
            comboBox.setValue(null);
        }

        return comboBox.getValue();
    }

    public Product getSelectedProduct() {

        return comboBox.getValue();
    }

    private void configure() {

        comboBox.setEditable(true);
        comboBox.setPromptText("Search by name, SKU, or barcode");
        comboBox.setItems(FXCollections.observableArrayList());
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Product product) {
                return formatProduct(product);
            }

            @Override
            public Product fromString(String string) {
                return findBestMatch(string);
            }
        });

        comboBox.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(
                    Product product,
                    boolean empty
            ) {

                super.updateItem(product, empty);

                setText(empty ? null : formatProduct(product));
            }
        });

        comboBox.getEditor().textProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (updatingEditor) {
                        return;
                    }
                    updateFilteredItems(newValue, true);
                }
        );

        comboBox.setOnAction(event -> {
            resolveSelectionFromEditor();
        });
    }

    private void updateFilteredItems(
            String query,
            boolean openPopup
    ) {

        String cleaned =
                clean(query);

        List<Product> filtered =
                filterProducts(cleaned);

        updatingEditor = true;
        try {
            comboBox.setItems(
                    FXCollections.observableArrayList(filtered)
            );

            if (cleaned.isEmpty()) {
                comboBox.getSelectionModel().clearSelection();
            }
        } finally {
            updatingEditor = false;
        }

        if (openPopup && comboBox.isFocused() && !filtered.isEmpty()) {
            comboBox.show();
        }
    }

    private List<Product> filterProducts(
            String query
    ) {

        if (query.isEmpty()) {
            return new ArrayList<>(masterProducts);
        }

        List<ProductScore> scored =
                new ArrayList<>();

        for (Product product : masterProducts) {
            int score = matchScore(product, query);
            if (score >= 0) {
                scored.add(new ProductScore(product, score));
            }
        }

        scored.sort(
                Comparator
                        .comparingInt(ProductScore::score)
                        .thenComparing(
                                score -> formatProduct(score.product()).toLowerCase(Locale.ROOT)
                        )
        );

        List<Product> filtered = new ArrayList<>();
        for (ProductScore score : scored) {
            filtered.add(score.product());
        }

        return filtered;
    }

    private Product findBestMatch(
            String query
    ) {

        String cleaned = clean(query);
        if (cleaned.isEmpty()) {
            return comboBox.getValue();
        }

        List<Product> filtered = filterProducts(cleaned);
        if (filtered.isEmpty()) {
            return null;
        }

        Product exactBarcode =
                filtered.stream()
                        .filter(product ->
                                clean(product.getBarcode()).equals(cleaned)
                        )
                        .findFirst()
                        .orElse(null);

        if (exactBarcode != null) {
            return exactBarcode;
        }

        Product exactSku =
                filtered.stream()
                        .filter(product ->
                                clean(product.getSku()).equals(cleaned)
                        )
                        .findFirst()
                        .orElse(null);

        if (exactSku != null) {
            return exactSku;
        }

        if (filtered.size() == 1) {
            return filtered.get(0);
        }

        return null;
    }

    private int matchScore(
            Product product,
            String query
    ) {

        if (product == null) {
            return -1;
        }

        String barcode = clean(product.getBarcode());
        String sku = clean(product.getSku());
        String name = clean(product.getName());

        if (!barcode.isEmpty() && barcode.equalsIgnoreCase(query)) {
            return 0;
        }

        if (!sku.isEmpty() && sku.equalsIgnoreCase(query)) {
            return 1;
        }

        if (!name.isEmpty() && name.toLowerCase(Locale.ROOT).contains(query)) {
            return 2;
        }

        return -1;
    }

    private String formatProduct(
            Product product
    ) {

        if (product == null) {
            return "";
        }

        StringBuilder label =
                new StringBuilder();

        if (product.getName() != null && !product.getName().trim().isEmpty()) {
            label.append(product.getName().trim());
        }

        if (product.getSku() != null && !product.getSku().trim().isEmpty()) {
            if (label.length() > 0) {
                label.append(" / ");
            }
            label.append(product.getSku().trim());
        }

        if (product.getBarcode() != null && !product.getBarcode().trim().isEmpty()) {
            label.append(" [").append(product.getBarcode().trim()).append("]");
        }

        return label.toString();
    }

    private String clean(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value.trim().toLowerCase(Locale.ROOT);
    }

    private record ProductScore(
            Product product,
            int score
    ) {
    }
}
