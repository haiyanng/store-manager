package com.storemanager.domain.attendance_anomaly.view;

import com.storemanager.domain.attendance.model.AttendanceSession;
import com.storemanager.domain.attendance_anomaly.model.AttendanceAnomaly;
import com.storemanager.domain.attendance_anomaly.model.AttendanceAnomalyFilter;
import com.storemanager.domain.attendance_anomaly.model.AttendanceAnomalySeverity;
import com.storemanager.domain.attendance_anomaly.model.AttendanceAnomalyStatus;
import com.storemanager.domain.attendance_anomaly.model.AttendanceAnomalyType;
import com.storemanager.domain.attendance_anomaly.presenter.AttendanceAnomalyPresenter;
import com.storemanager.domain.branch.model.Branch;
import com.storemanager.domain.employee.model.Employee;
import com.storemanager.core.util.TimeFormatUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class AttendanceAnomalyListController {

    @FXML
    private ComboBox<AttendanceAnomalyStatus> statusFilterComboBox;

    @FXML
    private ComboBox<AttendanceAnomalyType> typeFilterComboBox;

    @FXML
    private ComboBox<AttendanceAnomalySeverity> severityFilterComboBox;

    @FXML
    private ComboBox<Employee> employeeFilterComboBox;

    @FXML
    private ComboBox<Branch> branchFilterComboBox;

    @FXML
    private DatePicker fromDatePicker;

    @FXML
    private DatePicker toDatePicker;

    @FXML
    private Button applyFilterButton;

    @FXML
    private Button clearFilterButton;

    @FXML
    private Button refreshButton;

    @FXML
    private TableView<AttendanceAnomaly> anomalyTable;

    @FXML
    private TableColumn<AttendanceAnomaly, Long> anomalyIdColumn;

    @FXML
    private TableColumn<AttendanceAnomaly, String> anomalyEmployeeColumn;

    @FXML
    private TableColumn<AttendanceAnomaly, String> anomalyBranchColumn;

    @FXML
    private TableColumn<AttendanceAnomaly, String> anomalyTypeColumn;

    @FXML
    private TableColumn<AttendanceAnomaly, String> anomalySeverityColumn;

    @FXML
    private TableColumn<AttendanceAnomaly, String> anomalyStatusColumn;

    @FXML
    private TableColumn<AttendanceAnomaly, String> anomalyCreatedAtColumn;

    @FXML
    private TableColumn<AttendanceAnomaly, String> anomalyMessageColumn;

    @FXML
    private AttendanceAnomalyFormController anomalyFormController;

    @FXML
    private VBox managerActionsBox;

    @FXML
    private VBox adminActionsBox;

    @FXML
    private Button submitReportButton;

    @FXML
    private Button resolveButton;

    @FXML
    private Button dismissButton;

    @FXML
    private Button saveAttendanceButton;

    @FXML
    private Button updateNotificationButton;

    @FXML
    private Label statusLabel;

    private AttendanceAnomalyPresenter presenter;

    @FXML
    public void initialize() {

        presenter =
                new AttendanceAnomalyPresenter(this);

        configureFilters();
        configureTable();
        anomalyFormController.setManagerEditable(false);
        anomalyFormController.setAdminEditable(false);
        anomalyFormController.setEmployeeNotificationVisible(true);
        presenter.initialize();
    }

    @FXML
    public void onApplyFilters() {

        presenter.refreshTable();
    }

    @FXML
    public void onClearFilters() {

        presenter.clearFilters();
    }

    @FXML
    public void onRefresh() {

        presenter.refreshTable();
    }

    @FXML
    public void onSubmitReport() {

        presenter.submitManagerReport();
    }

    @FXML
    public void onResolve() {

        presenter.resolveSelected();
    }

    @FXML
    public void onDismiss() {

        presenter.dismissSelected();
    }

    @FXML
    public void onSaveAttendanceEdits() {

        presenter.saveAttendanceEdits();
    }

    @FXML
    public void onUpdateEmployeeNotification() {

        presenter.updateEmployeeNotification();
    }

    public AttendanceAnomalyFilter readFilter() {

        AttendanceAnomalyFilter filter =
                new AttendanceAnomalyFilter();

        filter.setStatus(statusFilterComboBox.getValue());
        filter.setType(typeFilterComboBox.getValue());
        filter.setSeverity(severityFilterComboBox.getValue());

        Employee employee =
                employeeFilterComboBox.getValue();
        if (employee != null) {
            filter.setEmployeeId(employee.getId());
        }

        Branch branch =
                branchFilterComboBox.getValue();
        if (branch != null) {
            filter.setBranchId(branch.getId());
        }

        filter.setFromDate(fromDatePicker.getValue());
        filter.setToDate(toDatePicker.getValue());
        return filter;
    }

    public void resetFilters() {

        statusFilterComboBox.setValue(null);
        typeFilterComboBox.setValue(null);
        severityFilterComboBox.setValue(null);
        employeeFilterComboBox.setValue(null);
        branchFilterComboBox.setValue(null);
        fromDatePicker.setValue(null);
        toDatePicker.setValue(null);
    }

    public void setAnomalies(
            List<AttendanceAnomaly> anomalies
    ) {

        anomalyTable.setItems(
                FXCollections.observableArrayList(anomalies)
        );
        anomalyTable.getSelectionModel().clearSelection();
        clearDetail();
    }

    public void setEmployees(
            List<Employee> employees
    ) {

        employeeFilterComboBox.setItems(
                FXCollections.observableArrayList(employees)
        );
    }

    public void setBranches(
            List<Branch> branches
    ) {

        branchFilterComboBox.setItems(
                FXCollections.observableArrayList(branches)
        );
    }

    public void setStatuses(
            List<AttendanceAnomalyStatus> statuses
    ) {

        statusFilterComboBox.setItems(
                FXCollections.observableArrayList(statuses)
        );
    }

    public void setTypes(
            List<AttendanceAnomalyType> types
    ) {

        typeFilterComboBox.setItems(
                FXCollections.observableArrayList(types)
        );
    }

    public void setSeverities(
            List<AttendanceAnomalySeverity> severities
    ) {

        severityFilterComboBox.setItems(
                FXCollections.observableArrayList(severities)
        );
    }

    public void showAnomaly(
            AttendanceAnomaly anomaly,
            AttendanceSession session,
            String employeeName,
            String branchName
    ) {

        anomalyFormController.showAnomaly(
                anomaly,
                session,
                employeeName,
                branchName
        );
    }

    public void clearDetail() {

        anomalyFormController.clear();
    }

    public void setBusy(
            boolean busy
    ) {

        anomalyTable.setDisable(busy);
        applyFilterButton.setDisable(busy);
        clearFilterButton.setDisable(busy);
        refreshButton.setDisable(busy);
        submitReportButton.setDisable(busy);
        resolveButton.setDisable(busy);
        dismissButton.setDisable(busy);
        saveAttendanceButton.setDisable(busy);
        updateNotificationButton.setDisable(busy);
        anomalyFormController.setBusy(busy);
    }

    public void setActionState(
            boolean hasSelection
    ) {

        submitReportButton.setDisable(!hasSelection);
        resolveButton.setDisable(!hasSelection);
        dismissButton.setDisable(!hasSelection);
        saveAttendanceButton.setDisable(!hasSelection);
        updateNotificationButton.setDisable(!hasSelection);
    }

    public void setManagerActionsVisible(
            boolean visible
    ) {

        managerActionsBox.setVisible(visible);
        managerActionsBox.setManaged(visible);
        anomalyFormController.setManagerEditable(visible);
    }

    public void setAdminActionsVisible(
            boolean visible
    ) {

        adminActionsBox.setVisible(visible);
        adminActionsBox.setManaged(visible);
        anomalyFormController.setAdminEditable(visible);
        anomalyFormController.setEmployeeNotificationVisible(visible);
    }

    public boolean isEmployeeNotificationEnabled() {

        return anomalyFormController.isEmployeeNotificationEnabled();
    }

    public String readManagerReportText() {

        return anomalyFormController.readManagerReportText();
    }

    public AttendanceSession readEditedAttendanceSession() {

        return anomalyFormController.readEditedAttendanceSession();
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

    private void configureFilters() {

        statusFilterComboBox.setPromptText("Status");
        typeFilterComboBox.setPromptText("Type");
        severityFilterComboBox.setPromptText("Severity");
        employeeFilterComboBox.setPromptText("Employee");
        branchFilterComboBox.setPromptText("Branch");

        employeeFilterComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Employee employee) {
                return employee == null ? "" : employee.toString();
            }

            @Override
            public Employee fromString(String value) {
                return null;
            }
        });

        branchFilterComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Branch branch) {
                return branch == null ? "" : branch.toString();
            }

            @Override
            public Branch fromString(String value) {
                return null;
            }
        });
    }

    private void configureTable() {

        anomalyIdColumn.setCellValueFactory(
                new PropertyValueFactory<>("id")
        );
        anomalyEmployeeColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        presenter.getEmployeeName(
                                cellData.getValue().getEmployeeId()
                        )
                )
        );
        anomalyBranchColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        presenter.getBranchName(
                                cellData.getValue().getBranchId()
                        )
                )
        );
        anomalyTypeColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        valueOrEmpty(cellData.getValue().getType())
                )
        );
        anomalySeverityColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        valueOrEmpty(cellData.getValue().getSeverity())
                )
        );
        anomalyStatusColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        valueOrEmpty(cellData.getValue().getStatus())
                )
        );
        anomalyCreatedAtColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        formatDateTime(cellData.getValue().getCreatedAt())
                )
        );
        anomalyMessageColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        cellData.getValue().getMessage()
                )
        );

        anomalyTable
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (observable, oldValue, newValue) ->
                                presenter.selectAnomaly(newValue)
                );
    }

    private String formatDateTime(
            LocalDateTime dateTime
    ) {

        return TimeFormatUtil.formatDateTime(dateTime);
    }

    private String valueOrEmpty(
            Object value
    ) {

        return value == null ? "" : value.toString();
    }
}
