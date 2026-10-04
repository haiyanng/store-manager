package com.storemanager.domain.user.view;

import com.storemanager.core.security.PermissionGuard;
import com.storemanager.core.util.UiFeedback;
import com.storemanager.domain.employee.model.Employee;
import com.storemanager.domain.user.model.RoleType;
import com.storemanager.domain.user.model.User;
import com.storemanager.domain.user.presenter.UserManagementPresenter;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;
import java.util.Objects;

public class UserManagementController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<RoleType> roleComboBox;
    @FXML private ComboBox<Employee> employeeComboBox;
    @FXML private Button createButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button clearLinkButton;
    @FXML private Button clearButton;
    @FXML private Button refreshButton;
    @FXML private Label statusLabel;
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Long> idColumn;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, RoleType> roleColumn;
    @FXML private TableColumn<User, String> employeeColumn;
    @FXML private TableColumn<User, Boolean> activeColumn;

    private UserManagementPresenter presenter;
    private User selectedUser;
    private List<Employee> employees = List.of();
    private boolean busy;

    @FXML public void initialize() {
        UiFeedback.emptyTable(usersTable, "No accounts found.");
        UiFeedback.booleanColumn(activeColumn, "Active", "Inactive");
        usersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        roleComboBox.setItems(FXCollections.observableArrayList(
                RoleType.OWNER, RoleType.MANAGER, RoleType.STAFF, RoleType.VIEWER));
        roleComboBox.setValue(RoleType.STAFF);
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        activeColumn.setCellValueFactory(new PropertyValueFactory<>("active"));
        employeeColumn.setCellValueFactory(cell -> {
            Employee employee = linkedEmployee(cell.getValue());
            return new SimpleStringProperty(employee == null ? "" : employee.toString());
        });
        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, old, user) -> select(user));
        setBusy(false);
        if (!PermissionGuard.canManageUsers()) {
            setStatus("You do not have permission to manage accounts");
            return;
        }
        presenter = new UserManagementPresenter(this);
        presenter.refresh();
    }

    @FXML public void onCreateUser() {
        presenter.create(usernameField.getText(), passwordField.getText(), roleComboBox.getValue(), employeeComboBox.getValue());
    }

    @FXML public void onEditUser() {
        RoleType role = roleComboBox.isDisabled() && selectedUser != null ? selectedUser.getRole() : roleComboBox.getValue();
        presenter.update(selectedUser, usernameField.getText(), passwordField.getText(), role, employeeComboBox.getValue());
    }

    @FXML public void onDeleteUser() {
        if (selectedUser == null) { showError("Select an account to delete"); return; }
        if (UiFeedback.confirm("Delete account?", "Delete account '" + selectedUser.getUsername() + "' and remove its employee link?")) {
            presenter.delete(selectedUser);
        }
    }

    @FXML public void onClearEmployeeLink() { employeeComboBox.setValue(null); }
    @FXML public void onClear() { clearForm(); }
    @FXML public void onRefresh() { presenter.refresh(); }

    public void setData(List<User> users, List<Employee> employees) {
        this.employees = List.copyOf(employees);
        usersTable.setItems(FXCollections.observableArrayList(users));
        clearForm();
    }

    private void select(User user) {
        selectedUser = user;
        usernameField.setText(user == null ? "" : user.getUsername());
        passwordField.clear();
        passwordField.setPromptText(user == null ? "Password" : "Leave blank to keep current password");
        roleComboBox.setValue(user == null ? RoleType.STAFF : user.getRole());
        refreshEmployeeChoices();
        employeeComboBox.setValue(user == null ? null : linkedEmployee(user));
        setBusy(busy);
    }

    public void clearForm() {
        usersTable.getSelectionModel().clearSelection();
        select(null);
    }

    private Employee linkedEmployee(User user) {
        if (user == null || user.getId() == null) return null;
        return employees.stream().filter(e -> Objects.equals(e.getUserId(), user.getId())).findFirst().orElse(null);
    }

    private void refreshEmployeeChoices() {
        Long selectedId = selectedUser == null ? null : selectedUser.getId();
        employeeComboBox.setItems(FXCollections.observableArrayList(employees.stream()
                .filter(e -> e.getUserId() == null || Objects.equals(e.getUserId(), selectedId)).toList()));
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
        boolean blocked = busy || !PermissionGuard.canManageUsers();
        boolean selected = selectedUser != null;
        usersTable.setDisable(blocked);
        usernameField.setDisable(blocked);
        passwordField.setDisable(blocked);
        roleComboBox.setDisable(blocked || selected && !PermissionGuard.canModifyRole(selectedUser, selectedUser.getRole()));
        employeeComboBox.setDisable(blocked || selected && !PermissionGuard.canEditUser(selectedUser));
        clearLinkButton.setDisable(employeeComboBox.isDisabled());
        createButton.setDisable(blocked || selected);
        editButton.setDisable(blocked || !selected || !PermissionGuard.canEditUser(selectedUser));
        deleteButton.setDisable(blocked || !selected || !PermissionGuard.canDeleteUser(selectedUser));
        clearButton.setDisable(blocked);
        refreshButton.setDisable(blocked);
    }

    public void setStatus(String message) { UiFeedback.status(statusLabel, message, usersTable); }
    public void showError(String message) { UiFeedback.showError(message); }
}
