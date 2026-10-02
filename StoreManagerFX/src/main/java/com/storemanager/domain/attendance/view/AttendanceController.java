package com.storemanager.domain.attendance.view;

import com.storemanager.core.util.UiFeedback;
import com.storemanager.domain.attendance.model.AttendanceMonthlyTotal;
import com.storemanager.domain.attendance.model.AttendanceSession;
import com.storemanager.domain.attendance.presenter.AttendancePresenter;
import com.storemanager.domain.employee.model.Employee;
import com.storemanager.core.util.TimeFormatUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;

import java.util.List;

public class AttendanceController {

    @FXML
    private ComboBox<Employee> employeeComboBox;

    @FXML
    private TableView<AttendanceSession> sessionTable;

    @FXML
    private TableColumn<AttendanceSession, Long> sessionIdColumn;

    @FXML
    private TableColumn<AttendanceSession, String> sessionEmployeeColumn;

    @FXML
    private TableColumn<AttendanceSession, String> checkInColumn;

    @FXML
    private TableColumn<AttendanceSession, String> checkOutColumn;

    @FXML
    private TableColumn<AttendanceSession, String> workedHoursColumn;

    @FXML
    private TableColumn<AttendanceSession, Long> createdByColumn;

    @FXML
    private TableView<AttendanceMonthlyTotal> monthlyTable;

    @FXML
    private TableColumn<AttendanceMonthlyTotal, String> monthlyEmployeeColumn;

    @FXML
    private TableColumn<AttendanceMonthlyTotal, String> monthlyMonthColumn;

    @FXML
    private TableColumn<AttendanceMonthlyTotal, String> monthlyHoursColumn;

    @FXML
    private Label statusLabel;

    @FXML
    private Label sessionStatusLabel;

    @FXML private javafx.scene.control.Button checkInButton;
    @FXML private javafx.scene.control.Button checkOutButton;
    @FXML private javafx.scene.control.Button refreshButton;
    private AttendancePresenter presenter;

    @FXML
    public void initialize() {
        UiFeedback.emptyTable(sessionTable, "No records found.");
        UiFeedback.emptyTable(monthlyTable, "No records found.");

        presenter =
                new AttendancePresenter(
                        this
                );

        configureEmployeeComboBox();

        configureSessionTable();
        configureMonthlyTable();
        configureSelectionListeners();

        presenter.initialize();
    }

    @FXML
    public void onCheckIn() {

        presenter.checkIn(
                employeeComboBox.getValue()
        );
    }

    @FXML
    public void onCheckOut() {

        presenter.checkOut(
                employeeComboBox.getValue()
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

        boolean selfService =
                presenter.isSelfServiceMode();

        employeeComboBox.setVisible(!selfService);
        employeeComboBox.setManaged(!selfService);
        employeeComboBox.setDisable(selfService);

        if (selfService) {
            employeeComboBox.setValue(
                    presenter.getCurrentEmployee()
            );
        }

        refreshSessionStatusLabel();
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
        employeeComboBox.setValue(presenter.isSelfServiceMode() ? presenter.getCurrentEmployee() : null);
        sessionTable.getSelectionModel().clearSelection();
        monthlyTable.getSelectionModel().clearSelection();
        refreshSessionStatusLabel();
    }

    public void setBusy(boolean busy) {
        employeeComboBox.setDisable(busy || presenter.isSelfServiceMode());
        sessionTable.setDisable(busy);
        monthlyTable.setDisable(busy);
        boolean missingEmployee = presenter.isSelfServiceMode() && presenter.getCurrentEmployee() == null;
        checkInButton.setDisable(busy || missingEmployee);
        checkOutButton.setDisable(busy || missingEmployee);
        refreshButton.setDisable(busy);
    }

    public void setStatus(
            String status
    ) {

        UiFeedback.status(statusLabel, status, sessionTable, monthlyTable);
    }

    public void refreshSessionStatusLabel() {
        sessionStatusLabel.setText(presenter.describeSessionStatus(employeeComboBox.getValue()));
    }

    public void showError(
            String message
    ) {
        UiFeedback.showError(message);
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

    private void configureSelectionListeners() {
        employeeComboBox.valueProperty().addListener((obs, old, value) -> refreshSessionStatusLabel());
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

        checkInColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        TimeFormatUtil.formatDateTime(
                                cellData.getValue().getCheckInTime()
                        )
                )
        );

        checkOutColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        TimeFormatUtil.formatDateTime(
                                cellData.getValue().getCheckOutTime()
                        )
                )
        );

        workedHoursColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        TimeFormatUtil.formatDurationHours(
                                cellData.getValue().getWorkedHours()
                        )
                )
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
                cellData -> new SimpleStringProperty(
                        TimeFormatUtil.formatDurationHours(
                                cellData.getValue().getTotalWorkedHours()
                        )
                )
        );
    }
}
