package com.storemanager.domain.attendance_anomaly.presenter;

import com.storemanager.core.runtime.BaseModulePresenter;
import com.storemanager.core.runtime.async.AsyncTaskRunner;
import com.storemanager.core.runtime.async.LoadingState;
import com.storemanager.core.session.AppSession;
import com.storemanager.domain.attendance.model.AttendanceSession;
import com.storemanager.domain.attendance_anomaly.model.AttendanceAnomaly;
import com.storemanager.domain.attendance_anomaly.model.AttendanceAnomalyFilter;
import com.storemanager.domain.attendance_anomaly.model.AttendanceAnomalySeverity;
import com.storemanager.domain.attendance_anomaly.model.AttendanceAnomalyStatus;
import com.storemanager.domain.attendance_anomaly.model.AttendanceAnomalyType;
import com.storemanager.domain.attendance_anomaly.service.AttendanceAnomalyService;
import com.storemanager.domain.attendance_anomaly.view.AttendanceAnomalyListController;
import com.storemanager.domain.employee.model.Employee;
import com.storemanager.domain.user.model.RoleType;
import com.storemanager.domain.user.model.User;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AttendanceAnomalyPresenter extends BaseModulePresenter {

    private final AttendanceAnomalyListController view;

    private final AttendanceAnomalyService anomalyService =
            new AttendanceAnomalyService();

    private LoadingState loadingState =
            LoadingState.IDLE;

    private AttendanceAnomaly selectedAnomaly;

    private Map<Long, Employee> employeesById = Map.of();

    public AttendanceAnomalyPresenter(
            AttendanceAnomalyListController view
    ) {

        this.view = view;
    }

    @Override
    public void initialize() {

        User currentUser = AppSession.getCurrentUser();
        if (currentUser == null) {
            view.showError("User session is required");
            return;
        }

        configureRoleVisibility(currentUser.getRole());
        loadLookups();
        refreshTable();
    }

    public void refreshTable() {

        AttendanceAnomalyFilter filter;
        try {
            filter = view.readFilter();
        } catch (IllegalArgumentException e) {
            view.showError(e.getMessage());
            return;
        }
        loadingState = LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus("Loading attendance anomalies...");

        AttendanceAnomaly previousSelection = selectedAnomaly;

        AsyncTaskRunner.run(
                () -> anomalyService.findReviewAnomalies(filter),
                anomalies -> {
                    selectedAnomaly = null;
                    view.setAnomalies(anomalies);
                    loadingState = LoadingState.SUCCESS;
                    view.setStatus("Ready");
                    restoreSelection(anomalies, previousSelection);
                },
                throwable -> {
                    loadingState = LoadingState.ERROR;
                    view.setStatus("Unable to load attendance anomalies");
                    view.showError(throwable.getMessage());
                },
                () -> view.setBusy(false)
        );
    }

    public void clearFilters() {

        view.resetFilters();
        refreshTable();
    }

    public void selectAnomaly(
            AttendanceAnomaly anomaly
    ) {

        selectedAnomaly = anomaly;

        if (anomaly == null) {
            view.clearDetail();
            view.setActionState(false);
            return;
        }

        AttendanceSession session =
                anomalyService.findAttendanceSessionById(
                        anomaly.getAttendanceSessionId()
                );

        view.showAnomaly(
                anomaly,
                session,
                getEmployeeName(anomaly.getEmployeeId())
        );
        view.setActionState(true);
    }

    public void submitManagerReport() {

        if (!hasSelection()) {
            view.showError("Select an anomaly first");
            return;
        }

        String reportText = view.readManagerReportText();

        loadingState = LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus("Submitting manager report...");

        AsyncTaskRunner.run(
                () -> anomalyService.submitManagerReport(
                        selectedAnomaly.getId(),
                        reportText
                ),
                success -> {
                    loadingState = LoadingState.SUCCESS;
                    refreshTable();
                },
                throwable -> {
                    loadingState = LoadingState.ERROR;
                    view.setStatus("Cannot submit manager report");
                    view.showError(throwable.getMessage());
                },
                () -> view.setBusy(false)
        );
    }

    public void resolveSelected() {

        if (!hasSelection()) {
            view.showError("Select an anomaly first");
            return;
        }

        loadingState = LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus("Resolving anomaly...");

        AsyncTaskRunner.run(
                () -> anomalyService.resolveAnomaly(selectedAnomaly.getId()),
                success -> {
                    loadingState = LoadingState.SUCCESS;
                    refreshTable();
                },
                throwable -> {
                    loadingState = LoadingState.ERROR;
                    view.setStatus("Cannot resolve anomaly");
                    view.showError(throwable.getMessage());
                },
                () -> view.setBusy(false)
        );
    }

    public void dismissSelected() {

        if (!hasSelection()) {
            view.showError("Select an anomaly first");
            return;
        }

        loadingState = LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus("Dismissing anomaly...");

        AsyncTaskRunner.run(
                () -> anomalyService.dismissAnomaly(selectedAnomaly.getId()),
                success -> {
                    loadingState = LoadingState.SUCCESS;
                    refreshTable();
                },
                throwable -> {
                    loadingState = LoadingState.ERROR;
                    view.setStatus("Cannot dismiss anomaly");
                    view.showError(throwable.getMessage());
                },
                () -> view.setBusy(false)
        );
    }

    public void updateEmployeeNotification() {

        if (!hasSelection()) {
            view.showError("Select an anomaly first");
            return;
        }

        boolean enabled = view.isEmployeeNotificationEnabled();

        loadingState = LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus("Updating employee notification...");

        AsyncTaskRunner.run(
                () -> anomalyService.updateEmployeeNotification(
                        selectedAnomaly.getId(),
                        enabled
                ),
                success -> {
                    loadingState = LoadingState.SUCCESS;
                    refreshTable();
                },
                throwable -> {
                    loadingState = LoadingState.ERROR;
                    view.setStatus("Cannot update employee notification");
                    view.showError(throwable.getMessage());
                },
                () -> view.setBusy(false)
        );
    }

    public void saveAttendanceEdits() {

        if (!hasSelection()) {
            view.showError("Select an anomaly first");
            return;
        }

        AttendanceSession session;
        try {
            session = view.readEditedAttendanceSession();
        } catch (Exception e) {
            view.showError(e.getMessage());
            return;
        }

        loadingState = LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus("Saving attendance edits...");

        AsyncTaskRunner.run(
                () -> anomalyService.updateAttendanceFromReview(
                        selectedAnomaly.getId(),
                        session
                ),
                success -> {
                    loadingState = LoadingState.SUCCESS;
                    refreshTable();
                },
                throwable -> {
                    loadingState = LoadingState.ERROR;
                    view.setStatus("Cannot save attendance edits");
                    view.showError(throwable.getMessage());
                },
                () -> view.setBusy(false)
        );
    }

    public String getEmployeeName(
            Long employeeId
    ) {

        Employee employee = employeesById.get(employeeId);
        if (employee == null || employee.getFullName() == null) {
            return employeeId == null ? "" : "Employee #" + employeeId;
        }

        return employee.toString();
    }

    public boolean hasSelection() {

        return selectedAnomaly != null
                && selectedAnomaly.getId() != null;
    }

    private void loadLookups() {

        List<Employee> employees =
                anomalyService.findReviewEmployees();

        employeesById =
                employees.stream()
                        .collect(
                                Collectors.toMap(
                                        Employee::getId,
                                        employee -> employee,
                                        (left, right) -> left
                                )
                        );

        view.setEmployees(employees);

        view.setStatuses(List.of(AttendanceAnomalyStatus.values()));
        view.setTypes(List.of(AttendanceAnomalyType.values()));
        view.setSeverities(List.of(AttendanceAnomalySeverity.values()));
    }

    private void configureRoleVisibility(
            RoleType role
    ) {

        boolean isAdmin =
                role == RoleType.OWNER;
        boolean isManager =
                role == RoleType.MANAGER;

        view.setManagerActionsVisible(isManager || isAdmin);
        view.setAdminActionsVisible(isAdmin);
    }

    private void restoreSelection(
            List<AttendanceAnomaly> anomalies,
            AttendanceAnomaly previousSelection
    ) {

        if (previousSelection == null || anomalies == null) {
            view.setActionState(false);
            return;
        }

        anomalies.stream()
                .filter(anomaly ->
                        previousSelection.getId() != null
                                && previousSelection.getId().equals(anomaly.getId())
                )
                .findFirst()
                .ifPresent(this::selectAnomaly);

        if (!hasSelection()) {
            view.setActionState(false);
        }
    }
}
