package com.storemanager.domain.payroll.view;

import com.storemanager.domain.employee.model.Employee;
import com.storemanager.domain.payroll.model.EmployeeSalaryConfig;
import com.storemanager.domain.payroll.model.PayrollRecord;
import com.storemanager.domain.payroll.presenter.PayrollPresenter;
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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class PayrollController {

    @FXML
    private ComboBox<Employee> employeeComboBox;

    @FXML
    private TextField hourlyRateField;

    @FXML
    private CheckBox activeCheckBox;

    @FXML
    private TextField yearField;

    @FXML
    private TextField monthField;

    @FXML
    private TableView<EmployeeSalaryConfig> salaryConfigTable;

    @FXML
    private TableColumn<EmployeeSalaryConfig, String> configEmployeeColumn;

    @FXML
    private TableColumn<EmployeeSalaryConfig, BigDecimal> configRateColumn;

    @FXML
    private TableColumn<EmployeeSalaryConfig, Boolean> configActiveColumn;

    @FXML
    private TableColumn<EmployeeSalaryConfig, LocalDateTime> configUpdatedAtColumn;

    @FXML
    private TableView<PayrollRecord> payrollTable;

    @FXML
    private TableColumn<PayrollRecord, String> payrollEmployeeColumn;

    @FXML
    private TableColumn<PayrollRecord, Integer> payrollMonthColumn;

    @FXML
    private TableColumn<PayrollRecord, Integer> payrollYearColumn;

    @FXML
    private TableColumn<PayrollRecord, BigDecimal> payrollHoursColumn;

    @FXML
    private TableColumn<PayrollRecord, BigDecimal> payrollRateColumn;

    @FXML
    private TableColumn<PayrollRecord, BigDecimal> payrollSalaryColumn;

    @FXML
    private TableColumn<PayrollRecord, LocalDateTime> payrollGeneratedAtColumn;

    @FXML
    private Label statusLabel;

    private PayrollPresenter presenter;

    @FXML
    public void initialize() {

        presenter =
                new PayrollPresenter(
                        this
                );

        activeCheckBox.setSelected(true);
        configureEmployeeComboBox();
        configureSalaryConfigTable();
        configurePayrollTable();

        presenter.initialize();
    }

    @FXML
    public void onSaveSalaryConfig() {

        presenter.saveSalaryConfig(
                employeeComboBox.getValue(),
                parseHourlyRate(),
                activeCheckBox.isSelected()
        );
    }

    @FXML
    public void onGeneratePayroll() {

        presenter.generatePayroll(
                parseYear(),
                parseMonth()
        );
    }

    @FXML
    public void onRefresh() {

        presenter.loadPayrollData();
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

    public void setSalaryConfigs(
            List<EmployeeSalaryConfig> configs
    ) {

        salaryConfigTable.setItems(
                FXCollections.observableArrayList(
                        configs
                )
        );
    }

    public void setPayrollRecords(
            List<PayrollRecord> records
    ) {

        payrollTable.setItems(
                FXCollections.observableArrayList(
                        records
                )
        );
    }

    public void setPeriod(
            int year,
            int month
    ) {

        yearField.setText(String.valueOf(year));
        monthField.setText(String.valueOf(month));
    }

    public void clearSalaryForm() {

        employeeComboBox.setValue(null);
        hourlyRateField.clear();
        activeCheckBox.setSelected(true);
    }

    public void setBusy(
            boolean busy
    ) {

        employeeComboBox.setDisable(busy);
        hourlyRateField.setDisable(busy);
        activeCheckBox.setDisable(busy);
        yearField.setDisable(busy);
        monthField.setDisable(busy);
        salaryConfigTable.setDisable(busy);
        payrollTable.setDisable(busy);
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
                new Alert(Alert.AlertType.ERROR);

        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void configureEmployeeComboBox() {

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
    }

    private void configureSalaryConfigTable() {

        configEmployeeColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                presenter.getEmployeeName(
                                        cellData.getValue().getEmployeeId()
                                )
                        )
        );
        configRateColumn.setCellValueFactory(
                new PropertyValueFactory<>("hourlyRate")
        );
        configActiveColumn.setCellValueFactory(
                new PropertyValueFactory<>("active")
        );
        configUpdatedAtColumn.setCellValueFactory(
                new PropertyValueFactory<>("updatedAt")
        );
    }

    private void configurePayrollTable() {

        payrollEmployeeColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                presenter.getEmployeeName(
                                        cellData.getValue().getEmployeeId()
                                )
                        )
        );
        payrollMonthColumn.setCellValueFactory(
                new PropertyValueFactory<>("month")
        );
        payrollYearColumn.setCellValueFactory(
                new PropertyValueFactory<>("year")
        );
        payrollHoursColumn.setCellValueFactory(
                new PropertyValueFactory<>("totalHours")
        );
        payrollRateColumn.setCellValueFactory(
                new PropertyValueFactory<>("hourlyRateSnapshot")
        );
        payrollSalaryColumn.setCellValueFactory(
                new PropertyValueFactory<>("totalSalary")
        );
        payrollGeneratedAtColumn.setCellValueFactory(
                new PropertyValueFactory<>("generatedAt")
        );
    }

    private BigDecimal parseHourlyRate() {

        String value =
                hourlyRateField.getText();

        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        return new BigDecimal(value.trim());
    }

    private int parseYear() {

        return Integer.parseInt(yearField.getText().trim());
    }

    private int parseMonth() {

        return Integer.parseInt(monthField.getText().trim());
    }
}
