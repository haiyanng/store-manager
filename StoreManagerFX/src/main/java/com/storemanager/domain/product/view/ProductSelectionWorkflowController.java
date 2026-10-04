package com.storemanager.domain.product.view;

import com.storemanager.core.util.UiFeedback;
import com.storemanager.domain.product.model.Product;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public class ProductSelectionWorkflowController {

    @FXML
    private ComboBox<Product> quickPickComboBox;

    @FXML
    private TextField searchField;

    @FXML
    private TableView<Product> resultTable;

    @FXML
    private TableColumn<Product, String> resultNameColumn;

    @FXML
    private TableColumn<Product, String> resultSkuColumn;

    @FXML
    private TableColumn<Product, String> resultStockColumn;

    @FXML
    private Label resultCountLabel;

    private final ObservableList<Product> masterProducts =
            FXCollections.observableArrayList();

    private final ObservableList<Product> quickPickProducts =
            FXCollections.observableArrayList();

    private Consumer<Product> selectedProductListener;

    private Map<Long, Integer> stockByProductId = Map.of();

    private boolean internalSelectionChange;

    public void setStockByProductId(Map<Long, Integer> stock) {
        stockByProductId = stock == null ? Map.of() : Map.copyOf(stock);
        resultStockColumn.setVisible(true);
        resultTable.refresh();
    }

    @FXML
    public void initialize() {
        UiFeedback.emptyTable(resultTable, "No products match your search.");


        configureQuickPickComboBox();
        configureResultTable();
        configureSearchField();

        resultCountLabel.setText("0 products");
    }

    public void setProducts(
            List<Product> products
    ) {

        masterProducts.setAll(
                products == null ? List.of() : products
        );

        runWithoutSelectionNotifications(
                () -> updateResults(
                        searchField.getText()
                )
        );

        notifySelectedProductChanged(
                resolveSelectedProduct()
        );
    }

    public void setQuickPickProducts(
            List<Product> products
    ) {

        quickPickProducts.setAll(
                products == null ? List.of() : products
        );
        quickPickComboBox.setItems(
                FXCollections.observableArrayList(
                        quickPickProducts
                )
        );
    }

    public void setSelectedProductListener(
            Consumer<Product> selectedProductListener
    ) {

        this.selectedProductListener = selectedProductListener;
    }

    public Product resolveSelectedProduct() {

        Product selectedResult =
                resultTable.getSelectionModel().getSelectedItem();

        if (selectedResult != null) {
            return selectedResult;
        }

        Product quickPick =
                quickPickComboBox.getValue();

        if (quickPick != null) {
            return quickPick;
        }

        if (resultTable.getItems() != null
                && resultTable.getItems().size() == 1) {
            return resultTable.getItems().get(0);
        }

        return null;
    }

    public void clearSelection() {

        runWithoutSelectionNotifications(
                () -> {
                    quickPickComboBox.setValue(null);
                    searchField.clear();
                    resultTable.getSelectionModel().clearSelection();
                    updateResults("");
                }
        );

        notifySelectedProductChanged(null);
    }

    public void setBusy(
            boolean busy
    ) {

        quickPickComboBox.setDisable(busy);
        searchField.setDisable(busy);
        resultTable.setDisable(busy);
    }

    private void configureQuickPickComboBox() {

        quickPickComboBox.setConverter(
                new StringConverter<>() {
                    @Override
                    public String toString(Product product) {
                        return formatProduct(product);
                    }

                    @Override
                    public Product fromString(String string) {
                        return null;
                    }
                }
        );

        quickPickComboBox.setCellFactory(listView -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(
                    Product product,
                    boolean empty
            ) {

                super.updateItem(product, empty);
                setText(empty ? null : formatProduct(product));
            }
        });

        quickPickComboBox.setOnAction(event -> {
            if (internalSelectionChange) {
                return;
            }

            runWithoutSelectionNotifications(
                    () -> {
                        resultTable.getSelectionModel().clearSelection();
                        notifySelectedProductChanged(
                                quickPickComboBox.getValue()
                        );
                    }
            );
        });
    }

    private void configureResultTable() {
        resultTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        resultStockColumn.setVisible(false);

        resultNameColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        cellData.getValue().getName()
                )
        );

        resultSkuColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        cellData.getValue().getSku()
                )
        );

        resultStockColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        Integer.toString(stockByProductId.getOrDefault(cellData.getValue().getId(), 0))
                )
        );

        resultTable.setRowFactory(tableView -> {
            TableRow<Product> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2
                        && !row.isEmpty()) {
                    resultTable.getSelectionModel().select(row.getItem());
                }
            });
            return row;
        });

        resultTable
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            if (internalSelectionChange) {
                                return;
                            }

                            if (newValue != null) {
                                runWithoutSelectionNotifications(
                                        () -> quickPickComboBox.setValue(null)
                                );
                            }

                            notifySelectedProductChanged(newValue);
                        }
                );
    }

    private void configureSearchField() {

        searchField.textProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (internalSelectionChange) {
                        return;
                    }

                    runWithoutSelectionNotifications(
                            () -> {
                                quickPickComboBox.setValue(null);
                                updateResults(newValue);
                            }
                    );

                    notifySelectedProductChanged(
                            resolveSelectedProduct()
                    );
                }
        );

        searchField.setOnAction(event -> {
            if (!resultTable.getItems().isEmpty()) {
                if (resultTable.getSelectionModel().isEmpty()) {
                    resultTable.getSelectionModel().select(0);
                }
                resultTable.requestFocus();
            }
        });
    }

    private void updateResults(
            String query
    ) {

        String cleaned = clean(query);
        List<Product> filtered = filterProducts(cleaned);

        resultTable.getSelectionModel().clearSelection();
        resultTable.setItems(
                FXCollections.observableArrayList(filtered)
        );

        resultCountLabel.setText(
                filtered.size() + " products"
        );
    }

    private List<Product> filterProducts(
            String query
    ) {

        if (query.isEmpty()) {
            return sortByLabel(masterProducts);
        }

        List<ProductScore> scored = new ArrayList<>();
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
                                score -> formatProduct(
                                        score.product()
                                ).toLowerCase(Locale.ROOT)
                        )
        );

        List<Product> filtered = new ArrayList<>();
        for (ProductScore score : scored) {
            filtered.add(score.product());
        }

        return filtered;
    }

    private List<Product> sortByLabel(
            List<Product> products
    ) {

        List<Product> sorted = new ArrayList<>(products);
        sorted.sort(
                Comparator.comparing(
                        product -> formatProduct(product)
                                .toLowerCase(Locale.ROOT)
                )
        );
        return sorted;
    }

    private int matchScore(
            Product product,
            String query
    ) {

        if (product == null) {
            return -1;
        }

        String sku = clean(product.getSku());
        String name = clean(product.getName());


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

        StringBuilder label = new StringBuilder();

        if (product.getName() != null
                && !product.getName().trim().isEmpty()) {
            label.append(product.getName().trim());
        }

        if (product.getSku() != null
                && !product.getSku().trim().isEmpty()) {
            if (label.length() > 0) {
                label.append(" / ");
            }
            label.append(product.getSku().trim());
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

    private void notifySelectedProductChanged(
            Product product
    ) {

        if (selectedProductListener != null) {
            selectedProductListener.accept(product);
        }
    }

    private void runWithoutSelectionNotifications(
            Runnable action
    ) {

        internalSelectionChange = true;
        try {
            action.run();
        } finally {
            internalSelectionChange = false;
        }
    }

    private record ProductScore(
            Product product,
            int score
    ) {
    }
}
