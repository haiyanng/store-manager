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
import com.storemanager.core.session.AppSession;

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

    private List<AttendanceSession> sessions =
            List.of();

    private Employee currentEmployee;

    private Branch currentActiveBranch;

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
                        attendanceService.getCurrentEmployee(),
                        attendanceService.findEmployees(),
                        attendanceService.findEmployeesById(),
                        attendanceService.findBranches(),
                        attendanceService.findBranchesById(),
                        attendanceService.findAllSessions(),
                        attendanceService.findCurrentMonthTotals()
                ),
                data -> {
                    currentEmployee =
                            data.currentEmployee();
                    employeesById =
                            data.employeesById();
                    branchesById =
                            data.branchesById();
                    sessions =
                            data.sessions();
                    currentActiveBranch =
                            resolveCurrentActiveBranch(data.branches());
                    view.setEmployees(data.employees());
                    view.setBranches(data.branches());
                    view.setSessions(sessions);
                    view.setMonthlyTotals(data.monthlyTotals());
                    view.refreshBranchContextLabel();
                    view.refreshSessionStatusLabel();
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

    public boolean isSelfServiceMode() {

        return attendanceService.isEmployeeSelfService();
    }

    public Employee getCurrentEmployee() {

        return currentEmployee;
    }

    public Branch getActiveBranch() {

        return currentActiveBranch;
    }

    public String getActiveBranchName() {

        if (currentActiveBranch != null
                && currentActiveBranch.getName() != null) {
            return currentActiveBranch.getName();
        }

        return AppSession.getActiveBranchName();
    }

    public String describeSessionStatus(
            Employee employee,
            Branch branch
    ) {

        Employee targetEmployee =
                employee != null ? employee : currentEmployee;

        if (targetEmployee == null) {
            return "Session: select an employee";
        }

        AttendanceSession openSession =
                sessions.stream()
                        .filter(session ->
                                targetEmployee.getId().equals(
                                        session.getEmployeeId()
                                )
                        )
                        .filter(session ->
                                branch == null
                                        || session.getBranchId() == null
                                        || session.getBranchId().equals(
                                        branch.getId()
                                )
                        )
                        .filter(session -> session.getCheckOutTime() == null)
                        .findFirst()
                        .orElse(null);

        if (openSession == null) {
            return "Session: no active session";
        }

        String branchName =
                getBranchName(openSession.getBranchId());

        return "Session: active since "
                + openSession.getCheckInTime()
                + " at "
                + branchName;
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
            Employee currentEmployee,
            List<Employee> employees,
            Map<Long, Employee> employeesById,
            List<Branch> branches,
            Map<Long, Branch> branchesById,
            List<AttendanceSession> sessions,
            List<AttendanceMonthlyTotal> monthlyTotals
    ) {
    }

    private Branch resolveCurrentActiveBranch(
            List<Branch> branches
    ) {

        Long activeBranchId =
                AppSession.getActiveBranchId();

        if (activeBranchId != null) {
            for (Branch branch : branches) {
                if (activeBranchId.equals(branch.getId())) {
                    return branch;
                }
            }
        }

        if (!branches.isEmpty()) {
            return branches.get(0);
        }

        return null;
    }
}
