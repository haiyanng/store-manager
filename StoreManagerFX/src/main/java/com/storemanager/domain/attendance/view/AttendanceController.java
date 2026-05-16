package com.storemanager.domain.attendance.view;

import com.storemanager.domain.attendance.model.AttendanceMonthlyTotal;
import com.storemanager.domain.attendance.model.AttendanceSession;
import com.storemanager.domain.attendance.presenter.AttendancePresenter;
import com.storemanager.domain.branch.model.Branch;
import com.storemanager.domain.employee.model.Employee;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class AttendanceController {

    @FXML
    private ComboBox<Employee> employeeComboBox;

    @FXML
    private ComboBox<Branch> branchComboBox;

    @FXML
    private TableView<AttendanceSession> sessionTable;

    @FXML
    private TableColumn<AttendanceSession, Long> sessionIdColumn;

    @FXML
    private TableColumn<AttendanceSession, String> sessionEmployeeColumn;

    @FXML
    private TableColumn<AttendanceSession, String> sessionBranchColumn;

    @FXML
    private TableColumn<AttendanceSession, LocalDateTime> checkInColumn;

    @FXML
    private TableColumn<AttendanceSession, LocalDateTime> checkOutColumn;

    @FXML
    private TableColumn<AttendanceSession, BigDecimal> workedHoursColumn;

    @FXML
    private TableColumn<AttendanceSession, Long> createdByColumn;

    @FXML
    private TableView<AttendanceMonthlyTotal> monthlyTable;

    @FXML
    private TableColumn<AttendanceMonthlyTotal, String> monthlyEmployeeColumn;

    @FXML
    private TableColumn<AttendanceMonthlyTotal, String> monthlyMonthColumn;

    @FXML
    private TableColumn<AttendanceMonthlyTotal, BigDecimal> monthlyHoursColumn;

    @FXML
    private Label statusLabel;

    private AttendancePresenter presenter;

    @FXML
    public void initialize() {

        presenter =
                new AttendancePresenter(
                        this
                );

        configureEmployeeComboBox();
        configureBranchComboBox();
        configureSessionTable();
        configureMonthlyTable();

        presenter.initialize();
    }

    @FXML
    public void onCheckIn() {

        presenter.checkIn(
                employeeComboBox.getValue(),
                branchComboBox.getValue()
        );
    }

    @FXML
    public void onCheckOut() {

        presenter.checkOut(
                employeeComboBox.getValue(),
                branchComboBox.getValue()
        );
    }

    @FXML
    public void onRefresh() {

        presenter.loadAttendanceData();
    }

    public void setEmployees(
            List<Employee> employees
    ) {

        employeeComboBox.setItems(
                FXCollections.observableArrayList(
                        employees
                )
        );
    }

    public void setBranches(
            List<Branch> branches
    ) {

        branchComboBox.setItems(
                FXCollections.observableArrayList(
                        branches
                )
        );
    }

    public void setSessions(
            List<AttendanceSession> sessions
    ) {

        sessionTable.setItems(
                FXCollections.observableArrayList(
                        sessions
                )
        );
    }

    public void setMonthlyTotals(
            List<AttendanceMonthlyTotal> totals
    ) {

        monthlyTable.setItems(
                FXCollections.observableArrayList(
                        totals
                )
        );
    }

    public void clearSelection() {

        employeeComboBox.setValue(null);
        branchComboBox.setValue(null);
        sessionTable.getSelectionModel().clearSelection();
        monthlyTable.getSelectionModel().clearSelection();
    }

    public void setBusy(
            boolean busy
    ) {

        employeeComboBox.setDisable(busy);
        branchComboBox.setDisable(busy);
        sessionTable.setDisable(busy);
        monthlyTable.setDisable(busy);
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

    private void configureEmployeeComboBox() {

        employeeComboBox.setConverter(
                new StringConverter<>() {
                    @Override
                    public String toString(
                            Employee employee
                    ) {

                        if (employee == null) {
                            return "";
                        }

                        return employee.toString();
                    }

                    @Override
                    public Employee fromString(
                            String value
                    ) {

                        return null;
                    }
                }
        );
    }

    private void configureBranchComboBox() {

        branchComboBox.setConverter(
                new StringConverter<>() {
                    @Override
                    public String toString(
                            Branch branch
                    ) {

                        if (branch == null) {
                            return "";
                        }

                        return branch.toString();
                    }

                    @Override
                    public Branch fromString(
                            String value
                    ) {

                        return null;
                    }
                }
        );
    }

    private void configureSessionTable() {

        sessionIdColumn.setCellValueFactory(
                new PropertyValueFactory<>("id")
        );

        sessionEmployeeColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                presenter.getEmployeeName(
                                        cellData.getValue().getEmployeeId()
                                )
                        )
        );

        sessionBranchColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                presenter.getBranchName(
                                        cellData.getValue().getBranchId()
                                )
                        )
        );

        checkInColumn.setCellValueFactory(
                new PropertyValueFactory<>("checkInTime")
        );

        checkOutColumn.setCellValueFactory(
                new PropertyValueFactory<>("checkOutTime")
        );

        workedHoursColumn.setCellValueFactory(
                new PropertyValueFactory<>("workedHours")
        );

        createdByColumn.setCellValueFactory(
                new PropertyValueFactory<>("createdByUserId")
        );
    }

    private void configureMonthlyTable() {

        monthlyEmployeeColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                presenter.getEmployeeName(
                                        cellData.getValue().getEmployeeId()
                                )
                        )
        );

        monthlyMonthColumn.setCellValueFactory(
                new PropertyValueFactory<>("month")
        );

        monthlyHoursColumn.setCellValueFactory(
                new PropertyValueFactory<>("totalWorkedHours")
        );
    }
}
