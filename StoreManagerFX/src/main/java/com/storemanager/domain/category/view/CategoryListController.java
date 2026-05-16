package com.storemanager.domain.category.view;

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

    private CategoryPresenter presenter;

    @FXML
    public void initialize() {

        presenter =
                new CategoryPresenter(
                        this
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

        presenter.saveCategory(
                categoryFormController.readCategory()
        );
    }

    @FXML
    public void onUpdate() {

        presenter.saveCategory(
                categoryFormController.readCategory()
        );
    }

    @FXML
    public void onDeactivate() {

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

    public void setUpdateEnabled(
            boolean enabled
    ) {

        updateButton.setDisable(!enabled);
    }

    public void setDeactivateEnabled(
            boolean enabled
    ) {

        deactivateButton.setDisable(!enabled);
    }

    public void setBusy(
            boolean busy
    ) {

        categoryTable.setDisable(busy);
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
