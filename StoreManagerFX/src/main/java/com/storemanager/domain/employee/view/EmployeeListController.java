package com.storemanager.domain.employee.view;

import com.storemanager.core.security.PermissionGuard;
import com.storemanager.core.util.UiFeedback;
import com.storemanager.domain.employee.model.Employee;
import com.storemanager.domain.employee.model.EmployeeListViewDto;
import com.storemanager.domain.employee.presenter.EmployeePresenter;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
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
    private TableColumn<EmployeeListViewDto, String> linkedUsernameColumn;

    @FXML
    private TableColumn<EmployeeListViewDto, String> linkedRoleColumn;

    @FXML
    private TableColumn<EmployeeListViewDto, Boolean> activeColumn;

    @FXML
    private Button updateButton;

    @FXML
    private Button deleteButton;

    private boolean busy;
    @FXML private Button createButton;
    @FXML private Button clearButton;
    @FXML private Button refreshButton;
    @FXML private javafx.scene.Node employeeForm;
    @FXML private javafx.scene.control.Label statusLabel;

    private EmployeePresenter presenter;

    @FXML
    public void initialize() {
        UiFeedback.emptyTable(employeeTable, "No employees yet. Use Create to add an employee.");
        UiFeedback.booleanColumn(activeColumn, "Active", "Inactive");

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
        try {

        presenter.saveEmployee(
                employeeFormController.readEmployee()
        );
        } catch (RuntimeException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void onUpdate() {
        try {

        presenter.saveEmployee(
                employeeFormController.readEmployee()
        );
        } catch (RuntimeException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void onDelete() {
        if (!UiFeedback.confirm("Delete employee?", "This permanently deletes the selected employee record.")) return;

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

    public void setUpdateEnabled(boolean enabled) {
        setBusy(busy);
    }

    public void setDeleteEnabled(boolean enabled) {
        setBusy(busy);
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
        boolean allowed = PermissionGuard.canViewEmployee();
        boolean selected = employeeTable.getSelectionModel().getSelectedItem() != null;
        employeeTable.setDisable(busy);
        employeeForm.setDisable(busy || !allowed);
        refreshButton.setDisable(busy);
        clearButton.setDisable(busy);
        createButton.setDisable(busy || !allowed || selected);
        updateButton.setDisable(busy || !allowed || !selected);
        deleteButton.setDisable(busy || !allowed || !selected);
    }

    public void setStatus(String status) {
        UiFeedback.status(statusLabel, status, employeeTable);
    }

    public void showError(
            String message
    ) {
        UiFeedback.showError(message);
    }
}
