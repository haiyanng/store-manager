package com.storemanager.domain.branch.presenter;

import com.storemanager.core.runtime.BaseCrudPresenter;
import com.storemanager.core.runtime.async.AsyncTaskRunner;
import com.storemanager.core.runtime.async.LoadingState;
import com.storemanager.domain.branch.model.Branch;
import com.storemanager.domain.branch.model.EmployeeBranchAssignment;
import com.storemanager.domain.branch.service.BranchService;
import com.storemanager.domain.branch.view.BranchController;
import com.storemanager.domain.employee.model.Employee;

import java.util.List;
import java.util.Map;

public class BranchPresenter extends BaseCrudPresenter<Branch> {

    private final BranchController view;

    private final BranchService branchService =
            new BranchService();

    private Map<Long, Branch> branchesById =
            Map.of();

    private Map<Long, Employee> employeesById =
            Map.of();

    private LoadingState loadingState =
            LoadingState.IDLE;

    public BranchPresenter(BranchController view) {

        this.view = view;
    }

    @Override
    public void initialize() {

        enterCreateMode();
        loadBranchData();
    }

    public void loadBranchData() {

        loadingState = LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus("Loading branches...");

        AsyncTaskRunner.run(
                () -> new BranchData(
                        branchService.findBranches(),
                        branchService.findEmployees(),
                        branchService.findAssignments(),
                        branchService.findBranchesById(),
                        branchService.findEmployeesById()
                ),
                data -> {
                    branchesById = data.branchesById();
                    employeesById = data.employeesById();
                    view.setBranches(data.branches());
                    view.setEmployees(data.employees());
                    view.setAssignments(data.assignments());
                    loadingState = LoadingState.SUCCESS;
                    view.setStatus("Ready");
                    updateActionState();
                },
                throwable -> {
                    loadingState = LoadingState.ERROR;
                    view.setStatus("Cannot load branches");
                    view.showError(throwable.getMessage());
                },
                () -> view.setBusy(false)
        );
    }

    public void selectBranch(Branch branch) {

        if (branch == null) {
            enterCreateMode();
            view.clearBranchForm();
            updateActionState();
            return;
        }

        enterEditMode(branch);
        view.showBranch(branch);
        updateActionState();
    }

    public void saveBranch(Branch branch) {

        try {
            if (hasSelection()) {
                branch.setId(getSelectedEntity().getId());
            }

            if (!branchService.saveBranch(branch)) {
                view.showError("Cannot save branch");
                return;
            }

            clearBranchForm();
            loadBranchData();

        } catch (Exception e) {
            view.showError(e.getMessage());
        }
    }

    public void assignEmployee(Employee employee, Branch branch) {

        try {
            if (!branchService.assignEmployee(employee, branch)) {
                view.showError("Cannot assign employee to branch");
                return;
            }

            view.clearAssignmentForm();
            loadBranchData();

        } catch (Exception e) {
            view.showError(e.getMessage());
        }
    }

    public void deactivateAssignment(EmployeeBranchAssignment assignment) {

        try {
            if (!branchService.deactivateAssignment(assignment)) {
                view.showError("Cannot deactivate assignment");
                return;
            }

            loadBranchData();

        } catch (Exception e) {
            view.showError(e.getMessage());
        }
    }

    public void clearBranchForm() {

        enterCreateMode();
        view.clearBranchSelection();
        view.clearBranchForm();
        updateActionState();
    }

    public String getBranchName(Long branchId) {

        Branch branch = branchesById.get(branchId);
        return branch == null ? "" : branch.getName();
    }

    public String getEmployeeName(Long employeeId) {

        Employee employee = employeesById.get(employeeId);
        return employee == null ? "" : employee.getFullName();
    }

    public LoadingState getLoadingState() {
        return loadingState;
    }

    private void updateActionState() {

        view.setUpdateEnabled(hasSelection());
    }

    private record BranchData(
            List<Branch> branches,
            List<Employee> employees,
            List<EmployeeBranchAssignment> assignments,
            Map<Long, Branch> branchesById,
            Map<Long, Employee> employeesById
    ) {
    }
}
