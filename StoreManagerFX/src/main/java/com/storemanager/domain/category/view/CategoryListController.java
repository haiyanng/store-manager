package com.storemanager.domain.category.view;

import com.storemanager.core.security.PermissionGuard;
import com.storemanager.core.util.UiFeedback;
import com.storemanager.domain.category.model.Category;
import com.storemanager.domain.category.presenter.CategoryPresenter;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class CategoryListController {

    @FXML
    private CategoryFormController categoryFormController;

    @FXML
    private TableView<Category> categoryTable;

    @FXML
    private TableColumn<Category, Long> idColumn;

    @FXML
    private TableColumn<Category, String> nameColumn;

    @FXML
    private TableColumn<Category, String> imageColumn;

    @FXML
    private TableColumn<Category, Boolean> activeColumn;

    @FXML
    private Button updateButton;

    @FXML
    private Button deactivateButton;

    @FXML
    private Label statusLabel;

    private boolean busy;
    @FXML private Button createButton;
    @FXML private Button clearButton;
    @FXML private Button refreshButton;
    @FXML private javafx.scene.Node categoryForm;

    private CategoryPresenter presenter;

    @FXML
    public void initialize() {
        UiFeedback.emptyTable(categoryTable, "No categories yet. Use Create to add a category.");
        UiFeedback.booleanColumn(activeColumn, "Active", "Inactive");


        presenter =
                new CategoryPresenter(
                        this
                );

        categoryTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );

        idColumn.setCellValueFactory(
                new PropertyValueFactory<>("id")
        );

        nameColumn.setCellValueFactory(
                new PropertyValueFactory<>("name")
        );

        imageColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                getImageStatus(
                                        cellData.getValue()
                                )
                        )
        );

        activeColumn.setCellValueFactory(
                new PropertyValueFactory<>("active")
        );

        categoryTable
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (observable, oldValue, newValue) ->
                                presenter.selectCategory(newValue)
                );

        presenter.initialize();
    }

    @FXML
    public void onCreate() {
        try {

        presenter.saveCategory(
                categoryFormController.readCategory()
        );
        } catch (RuntimeException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void onUpdate() {
        try {

        presenter.saveCategory(
                categoryFormController.readCategory()
        );
        } catch (RuntimeException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void onDeactivate() {
        if (!UiFeedback.confirm("Deactivate category?", "The selected category will become inactive. Existing transaction history is retained.")) return;

        presenter.deactivateCategory();
    }

    @FXML
    public void onClear() {

        presenter.clearForm();
    }

    @FXML
    public void onRefresh() {

        presenter.refreshTable();
    }

    public void setCategories(
            List<Category> categories
    ) {

        categoryTable.setItems(
                FXCollections.observableArrayList(
                        categories
                )
        );
    }

    public void showCategory(
            Category category
    ) {

        categoryFormController.showCategory(
                category
        );
    }

    public void clearSelection() {

        categoryTable.getSelectionModel().clearSelection();
    }

    public void clearCategoryForm() {

        categoryFormController.clear();
    }

    public void setUpdateEnabled(boolean enabled) {
        setBusy(busy);
    }

    public void setDeactivateEnabled(boolean enabled) {
        setBusy(busy);
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
        boolean allowed = PermissionGuard.canModifyProduct();
        boolean selected = categoryTable.getSelectionModel().getSelectedItem() != null;
        categoryTable.setDisable(busy);
        categoryForm.setDisable(busy || !allowed);
        refreshButton.setDisable(busy);
        clearButton.setDisable(busy);
        createButton.setDisable(busy || !allowed || selected);
        updateButton.setDisable(busy || !allowed || !selected);
        deactivateButton.setDisable(busy || !allowed || !selected);
    }

    public void setStatus(
            String status
    ) {

        UiFeedback.status(statusLabel, status, categoryTable);
    }

    public void showError(
            String message
    ) {
        UiFeedback.showError(message);
    }

    private String getImageStatus(
            Category category
    ) {

        if (category == null
                || category.getImagePath() == null
                || category.getImagePath().trim().isEmpty()) {
            return "";
        }

        return "Attached";
    }
}
