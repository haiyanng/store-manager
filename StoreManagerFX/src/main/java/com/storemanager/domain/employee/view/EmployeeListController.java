package com.storemanager.domain.employee.view;

import com.storemanager.domain.employee.model.Employee;
import com.storemanager.domain.employee.presenter.EmployeePresenter;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class EmployeeListController {

    @FXML
    private EmployeeFormController employeeFormController;

    @FXML
    private TableView<Employee> employeeTable;

    @FXML
    private TableColumn<Employee, Long> idColumn;

    @FXML
    private TableColumn<Employee, String> fullNameColumn;

    @FXML
    private TableColumn<Employee, String> phoneColumn;

    @FXML
    private TableColumn<Employee, String> positionColumn;

    @FXML
    private TableColumn<Employee, String> linkedUsernameColumn;

    @FXML
    private TableColumn<Employee, String> linkedRoleColumn;

    @FXML
    private TableColumn<Employee, Boolean> activeColumn;

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

        idColumn.setCellValueFactory(
                new PropertyValueFactory<>("id")
        );

        fullNameColumn.setCellValueFactory(
                new PropertyValueFactory<>("fullName")
        );

        phoneColumn.setCellValueFactory(
                new PropertyValueFactory<>("phone")
        );

        positionColumn.setCellValueFactory(
                new PropertyValueFactory<>("position")
        );

        linkedUsernameColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                presenter.getLinkedUsername(
                                        cellData.getValue()
                                )
                        )
        );

        linkedRoleColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                presenter.getLinkedRole(
                                        cellData.getValue()
                                )
                        )
        );

        activeColumn.setCellValueFactory(
                new PropertyValueFactory<>("active")
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
            List<Employee> employees
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
