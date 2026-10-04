package com.storemanager.domain.attendance.presenter;

import com.storemanager.core.runtime.BaseModulePresenter;
import com.storemanager.core.runtime.async.AsyncTaskRunner;
import com.storemanager.core.runtime.async.LoadingState;
import com.storemanager.domain.attendance.model.AttendanceMonthlyTotal;
import com.storemanager.domain.attendance.model.AttendanceSession;
import com.storemanager.domain.attendance.service.AttendanceService;
import com.storemanager.domain.attendance.view.AttendanceController;
import com.storemanager.domain.employee.model.Employee;

import java.util.List;
import java.util.Map;

public class AttendancePresenter extends BaseModulePresenter {

    private final AttendanceController view;

    private final AttendanceService attendanceService =
            new AttendanceService();

    private Map<Long, Employee> employeesById =
            Map.of();

    private List<AttendanceSession> sessions =
            List.of();

    private Employee currentEmployee;

    private LoadingState loadingState =
            LoadingState.IDLE;

    public AttendancePresenter(
            AttendanceController view
    ) {

        this.view = view;
    }

    @Override
    public void initialize() {

        loadAttendanceData();
    }

    public void loadAttendanceData() {
        if (loadingState == LoadingState.LOADING) return;

        loadingState =
                LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus("Loading attendance...");

        AsyncTaskRunner.run(
                () -> new AttendanceData(
                        attendanceService.getCurrentEmployee(),
                        attendanceService.findEmployees(),
                        attendanceService.findEmployeesById(),
                        attendanceService.findAllSessions(),
                        attendanceService.findCurrentMonthTotals()
                ),
                data -> {
                    currentEmployee =
                            data.currentEmployee();
                    employeesById =
                            data.employeesById();
                    sessions =
                            data.sessions();
                    view.setEmployees(data.employees());
                    view.setSessions(sessions);
                    view.setMonthlyTotals(data.monthlyTotals());
                    view.refreshSessionStatusLabel();
                    loadingState =
                            LoadingState.SUCCESS;
                    view.setStatus("Ready");
                },
                throwable -> {
                    loadingState =
                            LoadingState.ERROR;
                    view.setStatus("Unable to load attendance");
                    view.showError(throwable.getMessage());
                },
                () -> view.setBusy(false)
        );
    }

    public void checkIn(
            Employee employee
    ) {

        runAttendanceAction(
                "Checking in...",
                () -> attendanceService.checkIn(employee),
                "Cannot check in employee"
        );
    }

    public void checkOut(
            Employee employee
    ) {

        runAttendanceAction(
                "Checking out...",
                () -> attendanceService.checkOut(employee),
                "Cannot check out employee"
        );
    }

    public String getEmployeeName(
            Long employeeId
    ) {

        if (employeeId == null) {
            return "";
        }

        Employee employee =
                employeesById.get(employeeId);

        if (employee == null) {
            return "";
        }

        return employee.getFullName();
    }

    public LoadingState getLoadingState() {
        return loadingState;
    }

    public boolean isSelfServiceMode() {

        return attendanceService.isEmployeeSelfService();
    }

    public Employee getCurrentEmployee() {

        return currentEmployee;
    }

    public String describeSessionStatus(Employee employee) {
        Employee target = employee != null ? employee : currentEmployee;
        if (target == null) return isSelfServiceMode()
                ? "No linked employee. Contact the owner." : "Session: select an employee";
        return sessions.stream().filter(s -> target.getId().equals(s.getEmployeeId()) && s.getCheckOutTime() == null)
                .findFirst().map(s -> "Session: active since " + com.storemanager.core.util.TimeFormatUtil.formatDateTime(s.getCheckInTime()))
                .orElse("Session: no active session");
    }

    private void runAttendanceAction(
            String loadingMessage,
            AttendanceAction action,
            String errorMessage
    ) {
        if (loadingState == LoadingState.LOADING) return;
        loadingState =
                LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus(loadingMessage);

        AsyncTaskRunner.run(
                action::run,
                success -> {
                    if (!success) {
                        loadingState = LoadingState.ERROR;
                        view.showError(errorMessage);
                        view.setStatus(errorMessage);
                        view.setBusy(false);
                        return;
                    }

                    loadingState = LoadingState.SUCCESS;
                    view.clearSelection();
                    loadAttendanceData();
                },
                throwable -> {
                    loadingState =
                            LoadingState.ERROR;
                    view.setStatus(errorMessage);
                    view.showError(throwable.getMessage());
                    view.setBusy(false);
                },
                null
        );
    }

    private interface AttendanceAction {

        boolean run();
    }

    private record AttendanceData(
            Employee currentEmployee,
            List<Employee> employees,
            Map<Long, Employee> employeesById,
            List<AttendanceSession> sessions,
            List<AttendanceMonthlyTotal> monthlyTotals
    ) {
    }

}
