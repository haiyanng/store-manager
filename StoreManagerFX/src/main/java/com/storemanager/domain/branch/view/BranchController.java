package com.storemanager.domain.branch.view;

import com.storemanager.domain.branch.model.Branch;
import com.storemanager.domain.branch.model.EmployeeBranchAssignment;
import com.storemanager.domain.branch.presenter.BranchPresenter;
import com.storemanager.domain.employee.model.Employee;
import com.storemanager.core.util.TimeFormatUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ButtonType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;

import java.time.LocalDateTime;
import java.util.List;

public class BranchController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField addressField;

    @FXML
    private CheckBox activeCheckBox;

    @FXML
    private ComboBox<Employee> employeeComboBox;

    @FXML
    private ComboBox<Branch> branchComboBox;

    @FXML
    private TableView<Branch> branchTable;

    @FXML
    private TableColumn<Branch, Long> branchIdColumn;

    @FXML
    private TableColumn<Branch, String> branchNameColumn;

    @FXML
    private TableColumn<Branch, String> branchAddressColumn;

    @FXML
    private TableColumn<Branch, Boolean> branchActiveColumn;

    @FXML
    private TableView<EmployeeBranchAssignment> assignmentTable;

    @FXML
    private TableColumn<EmployeeBranchAssignment, String> assignmentEmployeeColumn;

    @FXML
    private TableColumn<EmployeeBranchAssignment, String> assignmentBranchColumn;

    @FXML
    private TableColumn<EmployeeBranchAssignment, Boolean> assignmentActiveColumn;

    @FXML
    private TableColumn<EmployeeBranchAssignment, String> assignmentAssignedAtColumn;

    @FXML
    private TableColumn<EmployeeBranchAssignment, Long> assignmentUserColumn;

    @FXML
    private Label statusLabel;

    private BranchPresenter presenter;

    @FXML
    public void initialize() {

        presenter = new BranchPresenter(this);

        activeCheckBox.setSelected(true);
        configureComboBoxes();
        configureBranchTable();
        configureAssignmentTable();

        presenter.initialize();
    }

    @FXML
    public void onCreateBranch() {

        presenter.saveBranch(readBranch());
    }

    @FXML
    public void onUpdateBranch() {

        presenter.saveBranch(readBranch());
    }

    @FXML
    public void onClearBranch() {

        presenter.clearBranchForm();
    }

    @FXML
    public void onAssignEmployee() {

        Employee employee =
                employeeComboBox.getValue();
        Branch branch =
                branchComboBox.getValue();

        if (!confirmTransferIfNeeded(employee, branch)) {
            return;
        }

        presenter.assignEmployee(
                employee,
                branch
        );
    }

    @FXML
    public void onDeactivateAssignment() {

        presenter.deactivateAssignment(
                assignmentTable.getSelectionModel().getSelectedItem()
        );
    }

    @FXML
    public void onRefresh() {

        presenter.loadBranchData();
    }

    public void setBranches(List<Branch> branches) {

        branchTable.setItems(FXCollections.observableArrayList(branches));
        branchComboBox.setItems(FXCollections.observableArrayList(branches));
    }

    public void setEmployees(List<Employee> employees) {

        employeeComboBox.setItems(FXCollections.observableArrayList(employees));
    }

    public void setAssignments(List<EmployeeBranchAssignment> assignments) {

        assignmentTable.setItems(
                FXCollections.observableArrayList(assignments)
        );
    }

    public void showBranch(Branch branch) {

        nameField.setText(branch.getName());
        addressField.setText(branch.getAddress());
        activeCheckBox.setSelected(branch.isActive());
    }

    public void clearBranchForm() {

        nameField.clear();
        addressField.clear();
        activeCheckBox.setSelected(true);
    }

    public void clearBranchSelection() {

        branchTable.getSelectionModel().clearSelection();
    }

    public void clearAssignmentForm() {

        employeeComboBox.setValue(null);
        branchComboBox.setValue(null);
        assignmentTable.getSelectionModel().clearSelection();
    }

    public void setUpdateEnabled(boolean enabled) {

        // Kept for presenter state symmetry; button stays usable through FXML.
    }

    public void setBusy(boolean busy) {

        branchTable.setDisable(busy);
        assignmentTable.setDisable(busy);
    }

    public void setStatus(String status) {

        statusLabel.setText(status);
    }

    public void showError(String message) {

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private boolean confirmTransferIfNeeded(
            Employee employee,
            Branch targetBranch
    ) {

        if (employee == null
                || employee.getId() == null
                || targetBranch == null
                || targetBranch.getId() == null) {
            return true;
        }

        String currentBranchName =
                presenter.getCurrentActiveBranchName(employee);

        if (currentBranchName == null
                || currentBranchName.isBlank()
                || currentBranchName.equals(targetBranch.getName())) {
            return true;
        }

        Alert alert =
                new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText(null);
        alert.setContentText(
                "This will transfer the employee from "
                        + currentBranchName
                        + " to "
                        + targetBranch.getName()
                        + "."
        );

        return alert.showAndWait()
                .filter(ButtonType.OK::equals)
                .isPresent();
    }

    private Branch readBranch() {

        Branch branch = new Branch();
        branch.setName(nameField.getText());
        branch.setAddress(addressField.getText());
        branch.setActive(activeCheckBox.isSelected());
        return branch;
    }

    private void configureComboBoxes() {

        employeeComboBox.setConverter(
                new StringConverter<>() {
                    @Override
                    public String toString(Employee employee) {
                        return employee == null ? "" : employee.toString();
                    }

                    @Override
                    public Employee fromString(String value) {
                        return null;
                    }
                }
        );

        branchComboBox.setConverter(
                new StringConverter<>() {
                    @Override
                    public String toString(Branch branch) {
                        return branch == null ? "" : branch.toString();
                    }

                    @Override
                    public Branch fromString(String value) {
                        return null;
                    }
                }
        );
    }

    private void configureBranchTable() {

        branchIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        branchNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        branchAddressColumn.setCellValueFactory(
                new PropertyValueFactory<>("address")
        );
        branchActiveColumn.setCellValueFactory(
                new PropertyValueFactory<>("active")
        );

        branchTable
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (observable, oldValue, newValue) ->
                                presenter.selectBranch(newValue)
                );
    }

    private void configureAssignmentTable() {

        assignmentEmployeeColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                presenter.getEmployeeName(
                                        cellData.getValue().getEmployeeId()
                                )
                        )
        );
        assignmentBranchColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                presenter.getBranchName(
                                        cellData.getValue().getBranchId()
                                )
                        )
        );
        assignmentActiveColumn.setCellValueFactory(
                new PropertyValueFactory<>("active")
        );
        assignmentAssignedAtColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        TimeFormatUtil.formatDateTime(
                                cellData.getValue().getAssignedAt()
                        )
                )
        );
        assignmentUserColumn.setCellValueFactory(
                new PropertyValueFactory<>("assignedByUserId")
        );
    }
}
