package com.storemanager.domain.employee.view;

import com.storemanager.domain.employee.model.Employee;
import com.storemanager.domain.employee.service.EmployeeService;
import com.storemanager.domain.user.model.User;
import com.storemanager.domain.user.repository.UserRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.Map;
import java.util.stream.Collectors;

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

    private final EmployeeService employeeService =
            new EmployeeService();

    private final UserRepository userRepository =
            new UserRepository();

    private Map<Long, User> usersById =
            Map.of();

    private Employee selectedEmployee;

    @FXML
    public void initialize() {

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
                                getLinkedUsername(
                                        cellData.getValue()
                                )
                        )
        );

        linkedRoleColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                getLinkedRole(
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
                                onEmployeeSelected(newValue)
                );

        refreshTable();
        updateActionState();
    }

    @FXML
    public void onCreate() {

        try {

            boolean success =
                    employeeService.create(
                            employeeFormController.readEmployee()
                    );

            if (!success) {
                showError("Cannot create employee");
                return;
            }

            clearSelection();
            refreshTable();

        } catch (Exception e) {

            showError(
                    e.getMessage()
            );
        }
    }

    @FXML
    public void onUpdate() {

        if (selectedEmployee == null) {
            showError("Select an employee to update");
            return;
        }

        try {

            Employee editedEmployee =
                    employeeFormController.readEmployee();

            editedEmployee.setId(
                    selectedEmployee.getId()
            );

            editedEmployee.setUserId(
                    selectedEmployee.getUserId()
            );

            boolean success =
                    employeeService.update(
                            editedEmployee
                    );

            if (!success) {
                showError("Cannot update employee");
                return;
            }

            clearSelection();
            refreshTable();

        } catch (Exception e) {

            showError(
                    e.getMessage()
            );
        }
    }

    @FXML
    public void onDelete() {

        if (selectedEmployee == null) {
            showError("Select an employee to delete");
            return;
        }

        try {

            boolean success =
                    employeeService.delete(
                            selectedEmployee
                    );

            if (!success) {
                showError("Cannot delete employee");
                return;
            }

            clearSelection();
            refreshTable();

        } catch (Exception e) {

            showError(
                    e.getMessage()
            );
        }
    }

    @FXML
    public void onRefresh() {

        refreshTable();
    }

    @FXML
    public void onClear() {

        clearSelection();
    }

    private void refreshTable() {

        usersById =
                userRepository
                        .findAll()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        User::getId,
                                        user -> user
                                )
                        );

        employeeTable.setItems(
                FXCollections.observableArrayList(
                        employeeService.findAll()
                )
        );

        updateActionState();
    }

    private void onEmployeeSelected(
            Employee employee
    ) {

        selectedEmployee = employee;

        employeeFormController.showEmployee(
                employee
        );

        updateActionState();
    }

    private void clearSelection() {

        selectedEmployee = null;
        employeeTable.getSelectionModel().clearSelection();
        employeeFormController.clear();
        updateActionState();
    }

    private void updateActionState() {

        boolean hasSelection =
                selectedEmployee != null;

        updateButton.setDisable(
                !hasSelection
        );

        deleteButton.setDisable(
                !hasSelection
        );
    }

    private void showError(
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

    private String getLinkedUsername(
            Employee employee
    ) {

        User user =
                getLinkedUser(employee);

        if (user == null) {
            return "";
        }

        return user.getUsername();
    }

    private String getLinkedRole(
            Employee employee
    ) {

        User user =
                getLinkedUser(employee);

        if (user == null) {
            return "";
        }

        return user.getRole().name();
    }

    private User getLinkedUser(
            Employee employee
    ) {

        if (employee == null || employee.getUserId() == null) {
            return null;
        }

        return usersById.get(
                employee.getUserId()
        );
    }
}
