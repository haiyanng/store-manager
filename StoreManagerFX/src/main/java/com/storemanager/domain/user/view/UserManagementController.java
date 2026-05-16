package com.storemanager.domain.user.view;

import com.storemanager.core.security.PermissionGuard;
import com.storemanager.domain.user.model.RoleType;
import com.storemanager.domain.user.model.User;
import com.storemanager.domain.user.service.UserManagementService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class UserManagementController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private ComboBox<RoleType> roleComboBox;

    @FXML
    private Button createButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    @FXML
    private TableView<User> usersTable;

    @FXML
    private TableColumn<User, Long> idColumn;

    @FXML
    private TableColumn<User, String> usernameColumn;

    @FXML
    private TableColumn<User, RoleType> roleColumn;

    @FXML
    private TableColumn<User, Boolean> activeColumn;

    private final UserManagementService userManagementService =
            new UserManagementService();

    private User selectedUser;

    @FXML
    public void initialize() {

        if (!PermissionGuard.canManageUsers()) {
            setActionsDisabled(true);
            usersTable.setDisable(true);
            showError("Bạn không có quyền truy cập quản lý người dùng");
            return;
        }

        roleComboBox.setItems(
                FXCollections.observableArrayList(
                        RoleType.OWNER,
                        RoleType.MANAGER,
                        RoleType.EMPLOYEE
                )
        );

        roleComboBox.setValue(
                RoleType.EMPLOYEE
        );

        idColumn.setCellValueFactory(
                new PropertyValueFactory<>("id")
        );

        usernameColumn.setCellValueFactory(
                new PropertyValueFactory<>("username")
        );

        roleColumn.setCellValueFactory(
                new PropertyValueFactory<>("role")
        );

        activeColumn.setCellValueFactory(
                new PropertyValueFactory<>("active")
        );

        usersTable
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (observable, oldValue, newValue) ->
                                onUserSelected(newValue)
                );

        updateActionState();

        refreshUsers();
    }

    @FXML
    public void onCreateUser() {

        try {

            userManagementService.createUser(
                    usernameField.getText(),
                    passwordField.getText(),
                    roleComboBox.getValue()
            );

            usernameField.clear();
            passwordField.clear();
            roleComboBox.setValue(
                    RoleType.EMPLOYEE
            );

            refreshUsers();
            clearForm();

        } catch (Exception e) {

            showError(
                    e.getMessage()
            );
        }
    }

    @FXML
    public void onEditUser() {

        if (selectedUser == null) {
            showError("Chọn người dùng cần sửa");
            return;
        }

        try {

            RoleType updatedRole =
                    roleComboBox.isDisabled()
                            ? selectedUser.getRole()
                            : roleComboBox.getValue();

            boolean success =
                    userManagementService.updateUser(
                            selectedUser,
                            usernameField.getText(),
                            passwordField.getText(),
                            updatedRole
                    );

            if (!success) {
                showError("Không thể cập nhật người dùng");
                return;
            }

            refreshUsers();
            clearForm();

        } catch (Exception e) {

            showError(
                    e.getMessage()
            );
        }
    }

    @FXML
    public void onDeleteUser() {

        if (selectedUser == null) {
            showError("Chọn người dùng cần xoá");
            return;
        }

        try {

            boolean success =
                    userManagementService.deleteUser(
                            selectedUser
                    );

            if (!success) {
                showError("Không thể xoá người dùng");
                return;
            }

            refreshUsers();
            clearForm();

        } catch (Exception e) {

            showError(
                    e.getMessage()
            );
        }
    }

    private void refreshUsers() {

        usersTable.setItems(
                FXCollections.observableArrayList(
                        userManagementService.findAll()
                )
        );

        updateActionState();
    }

    private void onUserSelected(
            User user
    ) {

        selectedUser = user;

        if (selectedUser == null) {
            resetFormFields();
            updateActionState();
            return;
        }

        usernameField.setText(
                selectedUser.getUsername()
        );

        passwordField.clear();

        roleComboBox.setValue(
                selectedUser.getRole()
        );

        roleComboBox.setDisable(
                PermissionGuard.isRootDeveloper(selectedUser)
                        || !PermissionGuard.canModifyRole(
                        selectedUser,
                        selectedUser.getRole()
                )
        );

        updateActionState();
    }

    private void clearForm() {

        selectedUser = null;
        usersTable.getSelectionModel().clearSelection();

        usernameField.clear();
        passwordField.clear();
        roleComboBox.setDisable(false);
        roleComboBox.setValue(
                RoleType.EMPLOYEE
        );

        updateActionState();
    }

    private void resetFormFields() {

        usernameField.clear();
        passwordField.clear();
        roleComboBox.setDisable(false);
        roleComboBox.setValue(
                RoleType.EMPLOYEE
        );
    }

    private void updateActionState() {

        boolean canManage =
                PermissionGuard.canManageUsers();

        createButton.setDisable(!canManage);

        editButton.setDisable(
                !canManage
                        || selectedUser == null
                        || !PermissionGuard.canEditUser(selectedUser)
        );

        deleteButton.setDisable(
                !canManage
                        || selectedUser == null
                        || !PermissionGuard.canDeleteUser(selectedUser)
        );
    }

    private void setActionsDisabled(
            boolean disabled
    ) {

        createButton.setDisable(disabled);
        editButton.setDisable(disabled);
        deleteButton.setDisable(disabled);
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
}
