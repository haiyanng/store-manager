package com.storemanager.domain.dashboard.view;

import com.storemanager.core.navigation.SceneManager;
import com.storemanager.core.session.AppSession;
import com.storemanager.domain.dashboard.model.DashboardAnalyticsSnapshot;
import com.storemanager.domain.dashboard.model.DashboardAttendanceHistoryRow;
import com.storemanager.domain.dashboard.model.DashboardBranchSummary;
import com.storemanager.domain.dashboard.model.DashboardCashFlowSummary;
import com.storemanager.domain.dashboard.model.DashboardEmployeeLocationRow;
import com.storemanager.domain.dashboard.model.DashboardInventoryAlertRow;
import com.storemanager.domain.dashboard.model.DashboardMetricCard;
import com.storemanager.domain.dashboard.model.DashboardPayrollSummary;
import com.storemanager.domain.dashboard.model.EmployeeDashboardDto;
import com.storemanager.domain.dashboard.model.ManagerDashboardDto;
import com.storemanager.domain.dashboard.model.OwnerDashboardDto;
import com.storemanager.domain.attendance.model.AttendanceRuntimeStatus;
import com.storemanager.domain.dashboard.presenter.DashboardAttendancePresenter;
import com.storemanager.domain.dashboard.presenter.DashboardAnalyticsPresenter;
import com.storemanager.core.util.TimeFormatUtil;
import com.storemanager.domain.user.model.User;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.List;

public class DashboardHomeController {

    @FXML
    private Label titleLabel;

    @FXML
    private Label subtitleLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private VBox quickAttendanceSection;

    @FXML
    private Label quickAttendanceStateLabel;

    @FXML
    private Label quickAttendanceBranchLabel;

    @FXML
    private Label quickAttendanceSessionLabel;

    @FXML
    private Label quickAttendanceTodayHoursLabel;

    @FXML
    private Label quickAttendanceLatestCheckInLabel;

    @FXML
    private Button quickCheckInButton;

    @FXML
    private Button quickCheckOutButton;

    @FXML
    private FlowPane metricsPane;

    @FXML
    private VBox employeeSection;

    @FXML
    private Label todayHoursLabel;

    @FXML
    private Label monthHoursLabel;

    @FXML
    private Label assignedBranchesLabel;

    @FXML
    private Label latestPayrollLabel;

    @FXML
    private TableView<DashboardAttendanceHistoryRow> attendanceHistoryTable;

    @FXML
    private TableColumn<DashboardAttendanceHistoryRow, String> historyDateColumn;

    @FXML
    private TableColumn<DashboardAttendanceHistoryRow, String> historyBranchColumn;

    @FXML
    private TableColumn<DashboardAttendanceHistoryRow, String> historyCheckInColumn;

    @FXML
    private TableColumn<DashboardAttendanceHistoryRow, String> historyCheckOutColumn;

    @FXML
    private TableColumn<DashboardAttendanceHistoryRow, String> historyHoursColumn;

    @FXML
    private VBox managerSection;

    @FXML
    private Label branchEmployeeCountLabel;

    @FXML
    private Label checkedInLabel;

    @FXML
    private Label attendanceOverviewLabel;

    @FXML
    private Label inventoryOverviewLabel;

    @FXML
    private Label salesOverviewLabel;

    @FXML
    private TableView<DashboardBranchSummary> branchSummaryTable;

    @FXML
    private TableColumn<DashboardBranchSummary, String> branchNameColumn;

    @FXML
    private TableColumn<DashboardBranchSummary, Long> branchSessionsColumn;

    @FXML
    private TableColumn<DashboardBranchSummary, BigDecimal> branchHoursColumn;

    @FXML
    private TableColumn<DashboardBranchSummary, Long> branchEmployeesColumn;

    @FXML
    private TableView<DashboardEmployeeLocationRow> locationTable;

    @FXML
    private TableColumn<DashboardEmployeeLocationRow, String> locationEmployeeColumn;

    @FXML
    private TableColumn<DashboardEmployeeLocationRow, String> locationBranchesColumn;

    @FXML
    private TableColumn<DashboardEmployeeLocationRow, Long> locationAssignmentsColumn;

    @FXML
    private VBox ownerSection;

    @FXML
    private Label totalEmployeesLabel;

    @FXML
    private Label activeEmployeesLabel;

    @FXML
    private Label workingEmployeesLabel;

    @FXML
    private Label payrollTotalsLabel;

    @FXML
    private Label revenueTotalsLabel;

    @FXML
    private Label inventoryAlertsLabel;

    @FXML
    private TableView<DashboardPayrollSummary> payrollTable;

    @FXML
    private TableColumn<DashboardPayrollSummary, String> payrollEmployeeColumn;

    @FXML
    private TableColumn<DashboardPayrollSummary, BigDecimal> payrollHoursColumn;

    @FXML
    private TableColumn<DashboardPayrollSummary, BigDecimal> payrollSalaryColumn;

    @FXML
    private TableView<DashboardCashFlowSummary> cashFlowTable;

    @FXML
    private TableColumn<DashboardCashFlowSummary, String> cashFlowLabelColumn;

    @FXML
    private TableColumn<DashboardCashFlowSummary, BigDecimal> cashFlowRevenueColumn;

    @FXML
    private TableColumn<DashboardCashFlowSummary, BigDecimal> cashFlowImportColumn;

    @FXML
    private TableColumn<DashboardCashFlowSummary, BigDecimal> cashFlowProductColumn;

    @FXML
    private TableColumn<DashboardCashFlowSummary, BigDecimal> cashFlowPayrollColumn;

    @FXML
    private TableColumn<DashboardCashFlowSummary, BigDecimal> cashFlowFinalColumn;

    @FXML
    private TableView<DashboardInventoryAlertRow> inventoryAlertTable;

    @FXML
    private TableColumn<DashboardInventoryAlertRow, String> inventoryProductColumn;

    @FXML
    private TableColumn<DashboardInventoryAlertRow, Long> inventoryQuantityColumn;

    @FXML
    private TableColumn<DashboardInventoryAlertRow, String> inventoryAlertColumn;

    @FXML
    private Label metricTitle0;

    @FXML
    private Label metricValue0;

    @FXML
    private Label metricDetail0;

    @FXML
    private VBox metricCard0;

    @FXML
    private Label metricTitle1;

    @FXML
    private Label metricValue1;

    @FXML
    private Label metricDetail1;

    @FXML
    private VBox metricCard1;

    @FXML
    private Label metricTitle2;

    @FXML
    private Label metricValue2;

    @FXML
    private Label metricDetail2;

    @FXML
    private VBox metricCard2;

    @FXML
    private Label metricTitle3;

    @FXML
    private Label metricValue3;

    @FXML
    private Label metricDetail3;

    @FXML
    private VBox metricCard3;

    @FXML
    private Label metricTitle4;

    @FXML
    private Label metricValue4;

    @FXML
    private Label metricDetail4;

    @FXML
    private VBox metricCard4;

    @FXML
    private Label metricTitle5;

    @FXML
    private Label metricValue5;

    @FXML
    private Label metricDetail5;

    @FXML
    private VBox metricCard5;

    private DashboardAnalyticsPresenter presenter;

    private DashboardAttendancePresenter attendancePresenter;

    @FXML
    public void initialize() {

        User currentUser = AppSession.getCurrentUser();
        if (currentUser == null) {
            SceneManager.switchScene("/fxml/auth/login.fxml");
            return;
        }

        presenter =
                new DashboardAnalyticsPresenter(this);

        attendancePresenter =
                new DashboardAttendancePresenter(this);

        configureTables();
        presenter.initialize();
        attendancePresenter.initialize();
    }

    public void renderSnapshot(
            DashboardAnalyticsSnapshot snapshot
    ) {

        if (snapshot == null) {
            setStatus("No dashboard data available");
            return;
        }

        titleLabel.setText(snapshot.getTitle());
        subtitleLabel.setText(snapshot.getSubtitle());

        hideAllSections();

        EmployeeDashboardDto employeeDashboard =
                snapshot.getEmployeeDashboard();
        ManagerDashboardDto managerDashboard =
                snapshot.getManagerDashboard();
        OwnerDashboardDto ownerDashboard =
                snapshot.getOwnerDashboard();

        if (employeeDashboard != null) {
            renderEmployeeDashboard(employeeDashboard);
            return;
        }

        if (managerDashboard != null) {
            renderManagerDashboard(managerDashboard);
            return;
        }

        if (ownerDashboard != null) {
            renderOwnerDashboard(ownerDashboard);
        }
    }

    public void setBusy(
            boolean busy
    ) {

        metricsPane.setDisable(busy);
        if (quickAttendanceSection != null) {
            quickAttendanceSection.setDisable(busy);
        }
        employeeSection.setDisable(busy);
        managerSection.setDisable(busy);
        ownerSection.setDisable(busy);
    }

    public void setQuickAttendanceBusy(
            boolean busy
    ) {

        if (quickAttendanceSection != null) {
            quickAttendanceSection.setDisable(busy);
        }
    }

    public void setStatus(
            String status
    ) {

        statusLabel.setText(status);
    }

    public void showError(
            String message
    ) {

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    public void onQuickCheckIn() {

        attendancePresenter.checkIn();
    }

    @FXML
    public void onQuickCheckOut() {

        attendancePresenter.checkOut();
    }

    public void renderQuickAttendance(
            AttendanceRuntimeStatus status
    ) {

        if (status == null) {
            quickAttendanceStateLabel.setText("🟠 Not working");
            quickAttendanceBranchLabel.setText("Branch: -");
            quickAttendanceSessionLabel.setText("Session: no active session");
            quickAttendanceTodayHoursLabel.setText("Today's worked hours: -");
            quickAttendanceLatestCheckInLabel.setText("Check-in: -");
            quickCheckInButton.setDisable(false);
            quickCheckOutButton.setDisable(true);
            return;
        }

        boolean working =
                status.getState() == com.storemanager.domain.attendance.model.AttendanceState.WORKING;

        quickAttendanceStateLabel.setText(
                working ? "🟢 Currently Working" : "🟠 Not working"
        );
        quickAttendanceBranchLabel.setText(
                "Branch: " + valueOrDefault(status.getActiveBranchName())
        );

        if (status.getActiveSession() == null) {
            quickAttendanceSessionLabel.setText(
                    "Session: no active session"
            );
        } else {
            quickAttendanceSessionLabel.setText(
                    "Session: active since "
                            + TimeFormatUtil.formatDateTime(
                                    status.getActiveSession().getCheckInTime()
                            )
            );
        }

        quickAttendanceTodayHoursLabel.setText(
                "Today's worked hours: "
                        + (status.getTodayWorkedHours() == null
                        ? "-"
                        : TimeFormatUtil.formatDurationHours(
                                status.getTodayWorkedHours()
                        ))
        );
        quickAttendanceLatestCheckInLabel.setText(
                "Check-in: "
                        + (status.getLatestCheckInTime() == null
                        ? "-"
                        : TimeFormatUtil.formatDateTime(
                                status.getLatestCheckInTime()
                        ))
        );

        quickCheckInButton.setDisable(working);
        quickCheckOutButton.setDisable(!working);
    }

    public void setQuickAttendanceStatus(
            String status
    ) {

        quickAttendanceStateLabel.setText(status);
    }

    public void onQuickAttendanceUpdated() {

        presenter.loadDashboard();
        attendancePresenter.loadQuickAttendance();
    }

    private void renderEmployeeDashboard(
            EmployeeDashboardDto dto
    ) {

        setSectionVisible(employeeSection, true);
        setSectionVisible(quickAttendanceSection, true);
        setSectionVisible(managerSection, false);
        setSectionVisible(ownerSection, false);

        renderMetrics(dto.getMetrics());

        todayHoursLabel.setText("Today's worked hours: " + valueOrDefault(dto.getTodayWorkedHours()));
        monthHoursLabel.setText("Current month worked hours: " + valueOrDefault(dto.getCurrentMonthWorkedHours()));
        assignedBranchesLabel.setText("Assigned branches: " + valueOrDefault(dto.getAssignedBranches()));
        latestPayrollLabel.setText(valueOrDefault(dto.getLatestPayrollSummary()));

        attendanceHistoryTable.setItems(
                FXCollections.observableArrayList(
                        dto.getRecentAttendanceHistory()
                )
        );
    }

    private void renderManagerDashboard(
            ManagerDashboardDto dto
    ) {

        setSectionVisible(employeeSection, false);
        setSectionVisible(quickAttendanceSection, false);
        setSectionVisible(managerSection, true);
        setSectionVisible(ownerSection, false);

        renderMetrics(dto.getMetrics());

        branchEmployeeCountLabel.setText("Branch employee count: " + dto.getBranchEmployeeCount());
        checkedInLabel.setText("Employees currently checked-in: " + dto.getEmployeesCurrentlyCheckedIn());
        attendanceOverviewLabel.setText("Attendance overview: " + valueOrDefault(dto.getAttendanceOverview()));
        inventoryOverviewLabel.setText("Branch inventory overview: " + valueOrDefault(dto.getInventoryOverview()));
        salesOverviewLabel.setText("Branch sales overview: " + valueOrDefault(dto.getSalesOverview()));

        branchSummaryTable.setItems(
                FXCollections.observableArrayList(
                        dto.getBranchOperationalSummaries()
                )
        );
        locationTable.setItems(
                FXCollections.observableArrayList(
                        dto.getEmployeeLocations()
                )
        );
    }

    private void renderOwnerDashboard(
            OwnerDashboardDto dto
    ) {

        setSectionVisible(employeeSection, false);
        setSectionVisible(quickAttendanceSection, false);
        setSectionVisible(managerSection, true);
        setSectionVisible(ownerSection, true);

        renderMetrics(dto.getMetrics());

        branchEmployeeCountLabel.setText("Branch employee count: " + dto.getBranchOperationalSummaries().stream().mapToLong(DashboardBranchSummary::getAssignedEmployees).sum());
        checkedInLabel.setText("Employees currently checked-in: " + dto.getEmployeesCurrentlyWorking());
        attendanceOverviewLabel.setText("Attendance overview: " + valueOrDefault(buildOwnerAttendanceOverview(dto)));
        inventoryOverviewLabel.setText("Branch inventory overview: " + valueOrDefault(buildOwnerInventoryOverview(dto)));
        salesOverviewLabel.setText("Branch sales overview: " + valueOrDefault(buildOwnerSalesOverview(dto)));

        branchSummaryTable.setItems(
                FXCollections.observableArrayList(
                        dto.getBranchOperationalSummaries()
                )
        );
        locationTable.setItems(
                FXCollections.observableArrayList(
                        dto.getEmployeeLocations()
                )
        );

        totalEmployeesLabel.setText("Total employees: " + dto.getTotalEmployees());
        activeEmployeesLabel.setText("Active employees: " + dto.getActiveEmployees());
        workingEmployeesLabel.setText("Employees currently working: " + dto.getEmployeesCurrentlyWorking());
        payrollTotalsLabel.setText("Payroll totals: " + valueOrDefault(dto.getPayrollTotals()));
        revenueTotalsLabel.setText("Revenue totals: " + valueOrDefault(dto.getRevenueTotals()));
        inventoryAlertsLabel.setText("Inventory alerts: " + valueOrDefault(dto.getInventoryAlertsSummary()));

        payrollTable.setItems(
                FXCollections.observableArrayList(
                        dto.getPayrollSummaries()
                )
        );
        cashFlowTable.setItems(
                FXCollections.observableArrayList(
                        dto.getCashFlowSummaries()
                )
        );
        inventoryAlertTable.setItems(
                FXCollections.observableArrayList(
                        dto.getInventoryAlerts()
                )
        );
    }

    private String buildOwnerAttendanceOverview(
            OwnerDashboardDto dto
    ) {

        return dto.getBranchOperationalSummaries().stream()
                .mapToLong(DashboardBranchSummary::getAttendanceSessions)
                .sum()
                + " sessions";
    }

    private String buildOwnerInventoryOverview(
            OwnerDashboardDto dto
    ) {

        return String.valueOf(dto.getInventoryAlerts().size()) + " low stock items";
    }

    private String buildOwnerSalesOverview(
            OwnerDashboardDto dto
    ) {

        return dto.getCashFlowSummaries().isEmpty()
                ? "No sales data available"
                : "Current month revenue "
                + dto.getCashFlowSummaries().get(0).getRevenueTotal();
    }

    private void hideAllSections() {

        setSectionVisible(employeeSection, false);
        setSectionVisible(quickAttendanceSection, false);
        setSectionVisible(managerSection, false);
        setSectionVisible(ownerSection, false);
    }

    private void setSectionVisible(
            VBox section,
            boolean visible
    ) {

        section.setVisible(visible);
        section.setManaged(visible);
    }

    private String valueOrDefault(
            String value
    ) {

        return value == null || value.trim().isEmpty()
                ? "-"
                : value;
    }

    private void configureTables() {

        attendanceHistoryTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );
        branchSummaryTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );
        locationTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );
        payrollTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );
        cashFlowTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );
        inventoryAlertTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );

        historyDateColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        cellData.getValue().getDateLabel()
                )
        );
        historyBranchColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        cellData.getValue().getBranchName()
                )
        );
        historyCheckInColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        cellData.getValue().getCheckInTime()
                )
        );
        historyCheckOutColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        cellData.getValue().getCheckOutTime()
                )
        );
        historyHoursColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        cellData.getValue().getWorkedHours()
                )
        );

        branchNameColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        cellData.getValue().getBranchName()
                )
        );
        branchSessionsColumn.setCellValueFactory(
                cellData -> new SimpleObjectProperty<>(
                        cellData.getValue().getAttendanceSessions()
                )
        );
        branchHoursColumn.setCellValueFactory(
                cellData -> new SimpleObjectProperty<>(
                        cellData.getValue().getAttendanceHours()
                )
        );
        branchEmployeesColumn.setCellValueFactory(
                cellData -> new SimpleObjectProperty<>(
                        cellData.getValue().getAssignedEmployees()
                )
        );

        locationEmployeeColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        cellData.getValue().getEmployeeName()
                )
        );
        locationBranchesColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        cellData.getValue().getBranches()
                )
        );
        locationAssignmentsColumn.setCellValueFactory(
                cellData -> new SimpleObjectProperty<>(
                        cellData.getValue().getActiveAssignments()
                )
        );

        payrollEmployeeColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        cellData.getValue().getEmployeeName()
                )
        );
        payrollHoursColumn.setCellValueFactory(
                cellData -> new SimpleObjectProperty<>(
                        cellData.getValue().getTotalHours()
                )
        );
        payrollSalaryColumn.setCellValueFactory(
                cellData -> new SimpleObjectProperty<>(
                        cellData.getValue().getTotalSalary()
                )
        );

        cashFlowLabelColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        cellData.getValue().getLabel()
                )
        );
        cashFlowRevenueColumn.setCellValueFactory(
                cellData -> new SimpleObjectProperty<>(
                        cellData.getValue().getRevenueTotal()
                )
        );
        cashFlowImportColumn.setCellValueFactory(
                cellData -> new SimpleObjectProperty<>(
                        cellData.getValue().getImportCost()
                )
        );
        cashFlowProductColumn.setCellValueFactory(
                cellData -> new SimpleObjectProperty<>(
                        cellData.getValue().getProductBusinessCashFlow()
                )
        );
        cashFlowPayrollColumn.setCellValueFactory(
                cellData -> new SimpleObjectProperty<>(
                        cellData.getValue().getPayrollCashFlow()
                )
        );
        cashFlowFinalColumn.setCellValueFactory(
                cellData -> new SimpleObjectProperty<>(
                        cellData.getValue().getFinalCashFlow()
                )
        );

        inventoryProductColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        cellData.getValue().getProductName()
                )
        );
        inventoryQuantityColumn.setCellValueFactory(
                cellData -> new SimpleObjectProperty<>(
                        cellData.getValue().getQuantity()
                )
        );
        inventoryAlertColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        cellData.getValue().getAlertText()
                )
        );
    }

    private void renderMetrics(
            List<DashboardMetricCard> cards
    ) {

        DashboardMetricCard[] metricCards = {
                cards.size() > 0 ? cards.get(0) : null,
                cards.size() > 1 ? cards.get(1) : null,
                cards.size() > 2 ? cards.get(2) : null,
                cards.size() > 3 ? cards.get(3) : null,
                cards.size() > 4 ? cards.get(4) : null,
                cards.size() > 5 ? cards.get(5) : null
        };

        VBox[] cardNodes = {
                metricCard0, metricCard1, metricCard2,
                metricCard3, metricCard4, metricCard5
        };

        Label[] titles = {
                metricTitle0, metricTitle1, metricTitle2,
                metricTitle3, metricTitle4, metricTitle5
        };

        Label[] values = {
                metricValue0, metricValue1, metricValue2,
                metricValue3, metricValue4, metricValue5
        };

        Label[] details = {
                metricDetail0, metricDetail1, metricDetail2,
                metricDetail3, metricDetail4, metricDetail5
        };

        for (int i = 0; i < cardNodes.length; i++) {
            DashboardMetricCard card = metricCards[i];

            boolean visible = card != null;
            cardNodes[i].setVisible(visible);
            cardNodes[i].setManaged(visible);

            if (card == null) {
                continue;
            }

            titles[i].setText(card.getTitle());
            values[i].setText(card.getValue());
            details[i].setText(card.getDetail());
        }
    }
}
