package com.storemanager.domain.employee.view;

import com.storemanager.domain.employee.model.Employee;
import com.storemanager.domain.employee.model.EmployeeListViewDto;
import com.storemanager.domain.employee.presenter.EmployeePresenter;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.List;

public class EmployeeListController {

    @FXML
    private EmployeeFormController employeeFormController;

    @FXML
    private TableView<EmployeeListViewDto> employeeTable;

    @FXML
    private TableColumn<EmployeeListViewDto, Long> idColumn;

    @FXML
    private TableColumn<EmployeeListViewDto, String> fullNameColumn;

    @FXML
    private TableColumn<EmployeeListViewDto, String> phoneColumn;

    @FXML
    private TableColumn<EmployeeListViewDto, String> positionColumn;

    @FXML
    private TableColumn<EmployeeListViewDto, String> branchColumn;

    @FXML
    private TableColumn<EmployeeListViewDto, String> linkedUsernameColumn;

    @FXML
    private TableColumn<EmployeeListViewDto, String> linkedRoleColumn;

    @FXML
    private TableColumn<EmployeeListViewDto, Boolean> activeColumn;

    @FXML
    private Button updateButton;

    @FXML
    private Button deleteButton;

    private EmployeePresenter presenter;

    @FXML
    public void initialize() {

        presenter =
                new EmployeePresenter(
                        this
                );

        employeeTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );

        idColumn.setCellValueFactory(
                new javafx.scene.control.cell.PropertyValueFactory<>("employeeId")
        );

        fullNameColumn.setCellValueFactory(
                new javafx.scene.control.cell.PropertyValueFactory<>("fullName")
        );

        phoneColumn.setCellValueFactory(
                new javafx.scene.control.cell.PropertyValueFactory<>("phone")
        );

        positionColumn.setCellValueFactory(
                new javafx.scene.control.cell.PropertyValueFactory<>("position")
        );

        branchColumn.setCellValueFactory(
                new javafx.scene.control.cell.PropertyValueFactory<>("branchDisplayName")
        );

        linkedUsernameColumn.setCellValueFactory(
                new javafx.scene.control.cell.PropertyValueFactory<>("linkedUsername")
        );

        linkedRoleColumn.setCellValueFactory(
                new javafx.scene.control.cell.PropertyValueFactory<>("linkedRole")
        );

        activeColumn.setCellValueFactory(
                new javafx.scene.control.cell.PropertyValueFactory<>("active")
        );

        employeeTable
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (observable, oldValue, newValue) ->
                        presenter.selectEmployee(newValue)
                );

        presenter.initialize();
    }

    @FXML
    public void onCreate() {

        presenter.saveEmployee(
                employeeFormController.readEmployee()
        );
    }

    @FXML
    public void onUpdate() {

        presenter.saveEmployee(
                employeeFormController.readEmployee()
        );
    }

    @FXML
    public void onDelete() {

        presenter.deleteEmployee();
    }

    @FXML
    public void onRefresh() {

        presenter.refreshTable();
    }

    @FXML
    public void onClear() {

        presenter.clearForm();
    }

    public void setEmployees(
            List<EmployeeListViewDto> employees
    ) {

        employeeTable.setItems(
                FXCollections.observableArrayList(
                        employees
                )
        );
    }

    public void showEmployee(
            Employee employee
    ) {

        employeeFormController.showEmployee(
                employee
        );
    }

    public void clearSelection() {

        employeeTable.getSelectionModel().clearSelection();
    }

    public void clearEmployeeForm() {

        employeeFormController.clear();
    }

    public void setUpdateEnabled(
            boolean enabled
    ) {

        updateButton.setDisable(
                !enabled
        );
    }

    public void setDeleteEnabled(
            boolean enabled
    ) {

        deleteButton.setDisable(
                !enabled
        );
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
