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
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class AttendanceAnomalyPresenter extends BaseModulePresenter {

    private final AttendanceAnomalyListController view;
    private final AttendanceAnomalyService anomalyService = new AttendanceAnomalyService();
    private LoadingState loadingState = LoadingState.IDLE;
    private AttendanceAnomaly selectedAnomaly;
    private Map<Long, Employee> employeesById = Map.of();
    private boolean lookupsLoaded;
    private boolean busy;
    private boolean detailLoading;
    private long selectionVersion;

    public AttendanceAnomalyPresenter(AttendanceAnomalyListController view) {
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
        view.setStatuses(List.of(AttendanceAnomalyStatus.values()));
        view.setTypes(List.of(AttendanceAnomalyType.values()));
        view.setSeverities(List.of(AttendanceAnomalySeverity.values()));
        refreshTable();
    }

    public void refreshTable() {
        refreshTable(false);
    }

    private void refreshTable(boolean changesSaved) {
        if (busy) return;
        AttendanceAnomalyFilter filter;
        try {
            filter = view.readFilter();
        } catch (IllegalArgumentException e) {
            view.showError(e.getMessage());
            return;
        }
        AttendanceAnomaly previousSelection = selectedAnomaly;
        boolean loadLookups = !lookupsLoaded;
        busy = true;
        loadingState = LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus("Loading attendance anomalies...");
        AsyncTaskRunner.run(() -> new ReviewData(
                        loadLookups ? anomalyService.findReviewEmployees() : null,
                        anomalyService.findReviewAnomalies(filter)),
                data -> {
                    if (data.employees() != null) {
                        employeesById = data.employees().stream().collect(Collectors.toMap(
                                Employee::getId, employee -> employee, (left, right) -> left));
                        view.setEmployees(data.employees());
                        lookupsLoaded = true;
                    }
                    selectedAnomaly = null;
                    selectionVersion++;
                    detailLoading = false;
                    view.setAnomalies(data.anomalies());
                    loadingState = LoadingState.SUCCESS;
                    view.setStatus("Ready");
                    restoreSelection(data.anomalies(), previousSelection);
                }, error -> {
                    loadingState = LoadingState.ERROR;
                    view.setStatus(changesSaved
                            ? "Changes saved; unable to refresh the list. Click Refresh to reload."
                            : "Unable to load attendance anomalies");
                    view.showError(error.getMessage());
                }, this::finishTask);
    }

    public void clearFilters() {
        if (busy) return;
        view.resetFilters();
        refreshTable();
    }

    public void selectAnomaly(AttendanceAnomaly anomaly) {
        if (busy) return;
        loadSelection(anomaly);
    }

    private void loadSelection(AttendanceAnomaly anomaly) {
        selectedAnomaly = anomaly;
        long requestVersion = ++selectionVersion;
        detailLoading = false;
        view.clearDetail();
        view.setActionState(false);
        if (anomaly == null) return;
        Long sessionId = anomaly.getAttendanceSessionId();
        detailLoading = true;
        view.setStatus("Loading attendance details...");
        AsyncTaskRunner.run(() -> anomalyService.findAttendanceSessionById(sessionId),
                session -> {
                    if (requestVersion != selectionVersion) return;
                    detailLoading = false;
                    view.showAnomaly(anomaly, session, getEmployeeName(anomaly.getEmployeeId()));
                    view.setActionState(!busy && hasSelection());
                    view.setStatus("Ready");
                }, error -> {
                    if (requestVersion != selectionVersion) return;
                    detailLoading = false;
                    selectedAnomaly = null;
                    view.setActionState(false);
                    view.setStatus("Unable to load attendance details");
                    view.showError(error.getMessage());
                }, null);
    }

    public void submitManagerReport() {
        if (!requireSelection()) return;
        Long anomalyId = selectedAnomaly.getId();
        String reportText = view.readManagerReportText();
        mutate(() -> anomalyService.submitManagerReport(anomalyId, reportText),
                "Submitting manager report...", "Cannot submit manager report");
    }

    public void resolveSelected() {
        if (!requireSelection()) return;
        Long anomalyId = selectedAnomaly.getId();
        mutate(() -> anomalyService.resolveAnomaly(anomalyId),
                "Resolving anomaly...", "Cannot resolve anomaly");
    }

    public void dismissSelected() {
        if (!requireSelection()) return;
        Long anomalyId = selectedAnomaly.getId();
        mutate(() -> anomalyService.dismissAnomaly(anomalyId),
                "Dismissing anomaly...", "Cannot dismiss anomaly");
    }

    public void updateEmployeeNotification() {
        if (!requireSelection()) return;
        Long anomalyId = selectedAnomaly.getId();
        boolean enabled = view.isEmployeeNotificationEnabled();
        mutate(() -> anomalyService.updateEmployeeNotification(anomalyId, enabled),
                "Updating employee notification...", "Cannot update employee notification");
    }

    public void saveAttendanceEdits() {
        if (!requireSelection()) return;
        AttendanceSession session;
        try {
            session = view.readEditedAttendanceSession();
        } catch (Exception e) {
            view.showError(e.getMessage());
            return;
        }
        Long anomalyId = selectedAnomaly.getId();
        mutate(() -> anomalyService.updateAttendanceFromReview(anomalyId, session),
                "Saving attendance edits...", "Cannot save attendance edits");
    }

    private void mutate(Callable<Boolean> task, String status, String failureStatus) {
        if (busy || detailLoading) return;
        busy = true;
        loadingState = LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus(status);
        boolean[] saved = {false};
        AsyncTaskRunner.run(task,
                success -> {
                    if (!success) {
                        loadingState = LoadingState.ERROR;
                        view.setStatus(failureStatus);
                        view.showError(failureStatus);
                        return;
                    }
                    saved[0] = true;
                    loadingState = LoadingState.SUCCESS;
                }, error -> {
                    loadingState = LoadingState.ERROR;
                    view.setStatus(failureStatus);
                    view.showError(error.getMessage());
                }, () -> {
                    finishTask();
                    if (saved[0]) refreshTable(true);
                });
    }

    private boolean requireSelection() {
        if (busy || detailLoading) return false;
        if (hasSelection()) return true;
        view.showError("Select an anomaly first");
        return false;
    }

    private void finishTask() {
        busy = false;
        view.setBusy(false);
        view.setActionState(hasSelection());
    }

    public String getEmployeeName(Long employeeId) {
        Employee employee = employeesById.get(employeeId);
        if (employee == null || employee.getFullName() == null) {
            return employeeId == null ? "" : "Employee #" + employeeId;
        }
        return employee.toString();
    }

    public boolean hasSelection() {
        return !detailLoading && selectedAnomaly != null && selectedAnomaly.getId() != null;
    }

    private void configureRoleVisibility(RoleType role) {
        boolean isAdmin = role == RoleType.OWNER;
        boolean isManager = role == RoleType.MANAGER;
        view.setManagerActionsVisible(isManager || isAdmin);
        view.setAdminActionsVisible(isAdmin);
    }

    private void restoreSelection(List<AttendanceAnomaly> anomalies, AttendanceAnomaly previousSelection) {
        AttendanceAnomaly selection = previousSelection == null ? null : anomalies.stream()
                .filter(anomaly -> previousSelection.getId() != null && previousSelection.getId().equals(anomaly.getId()))
                .findFirst().orElse(null);
        loadSelection(selection);
    }

    private record ReviewData(List<Employee> employees, List<AttendanceAnomaly> anomalies) {
    }
}
