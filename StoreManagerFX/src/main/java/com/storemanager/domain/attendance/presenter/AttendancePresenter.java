package com.storemanager.domain.attendance.presenter;

import com.storemanager.core.runtime.BaseModulePresenter;
import com.storemanager.core.runtime.async.AsyncTaskRunner;
import com.storemanager.core.runtime.async.LoadingState;
import com.storemanager.domain.attendance.model.AttendanceMonthlyTotal;
import com.storemanager.domain.attendance.model.AttendanceSession;
import com.storemanager.domain.attendance.service.AttendanceService;
import com.storemanager.domain.attendance.view.AttendanceController;
import com.storemanager.domain.branch.model.Branch;
import com.storemanager.domain.employee.model.Employee;

import java.util.List;
import java.util.Map;

public class AttendancePresenter extends BaseModulePresenter {

    private final AttendanceController view;

    private final AttendanceService attendanceService =
            new AttendanceService();

    private Map<Long, Employee> employeesById =
            Map.of();

    private Map<Long, Branch> branchesById =
            Map.of();

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

        loadingState =
                LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus("Loading attendance...");

        AsyncTaskRunner.run(
                () -> new AttendanceData(
                        attendanceService.findEmployees(),
                        attendanceService.findEmployeesById(),
                        attendanceService.findBranches(),
                        attendanceService.findBranchesById(),
                        attendanceService.findAllSessions(),
                        attendanceService.findCurrentMonthTotals()
                ),
                data -> {
                    employeesById =
                            data.employeesById();
                    branchesById =
                            data.branchesById();
                    view.setEmployees(data.employees());
                    view.setBranches(data.branches());
                    view.setSessions(data.sessions());
                    view.setMonthlyTotals(data.monthlyTotals());
                    loadingState =
                            LoadingState.SUCCESS;
                    view.setStatus("Ready");
                },
                throwable -> {
                    loadingState =
                            LoadingState.ERROR;
                    view.setStatus("Cannot load attendance");
                    view.showError(throwable.getMessage());
                },
                () -> view.setBusy(false)
        );
    }

    public void checkIn(
            Employee employee,
            Branch branch
    ) {

        runAttendanceAction(
                "Checking in...",
                () -> attendanceService.checkIn(employee, branch),
                "Cannot check in employee"
        );
    }

    public void checkOut(
            Employee employee,
            Branch branch
    ) {

        runAttendanceAction(
                "Checking out...",
                () -> attendanceService.checkOut(employee, branch),
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

    public String getBranchName(
            Long branchId
    ) {

        if (branchId == null) {
            return "";
        }

        Branch branch =
                branchesById.get(branchId);

        if (branch == null) {
            return "";
        }

        return branch.getName();
    }

    public LoadingState getLoadingState() {
        return loadingState;
    }

    private void runAttendanceAction(
            String loadingMessage,
            AttendanceAction action,
            String errorMessage
    ) {

        loadingState =
                LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus(loadingMessage);

        AsyncTaskRunner.run(
                action::run,
                success -> {
                    if (!success) {
                        view.showError(errorMessage);
                        view.setStatus(errorMessage);
                        view.setBusy(false);
                        return;
                    }

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
            List<Employee> employees,
            Map<Long, Employee> employeesById,
            List<Branch> branches,
            Map<Long, Branch> branchesById,
            List<AttendanceSession> sessions,
            List<AttendanceMonthlyTotal> monthlyTotals
    ) {
    }
}
