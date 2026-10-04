package com.storemanager.domain.employee.presenter;

import com.storemanager.core.runtime.BaseCrudPresenter;
import com.storemanager.core.runtime.CrudMode;
import com.storemanager.core.runtime.async.AsyncTaskRunner;
import com.storemanager.domain.employee.model.EmployeeListViewDto;
import com.storemanager.domain.employee.model.Employee;
import com.storemanager.domain.employee.service.EmployeeService;
import com.storemanager.domain.employee.view.EmployeeListController;

import java.util.List;
import java.util.concurrent.Callable;

public class EmployeePresenter extends BaseCrudPresenter<Employee> {

    private final EmployeeListController view;
    private final EmployeeService employeeService = new EmployeeService();
    private boolean busy;

    public EmployeePresenter(EmployeeListController view) {
        this.view = view;
    }

    @Override
    public void initialize() {
        enterCreateMode();
        loadEmployees();
    }

    public void loadEmployees() {
        refreshTable();
    }

    public void refreshTable() {
        runTask(employeeService::findEmployeeListViews, "Loading employees...", "Unable to load employees");
    }

    public void selectEmployee(EmployeeListViewDto employeeRow) {
        if (busy) return;
        if (employeeRow == null || employeeRow.getEmployeeId() == null) {
            enterCreateMode();
            view.clearEmployeeForm();
            updateActionState();
            return;
        }

        Long employeeId = employeeRow.getEmployeeId();
        busy = true;
        view.setBusy(true);
        view.setStatus("Loading employee...");
        AsyncTaskRunner.run(() -> employeeService.findById(employeeId),
                employee -> {
                    if (employee == null) {
                        clearForm();
                        view.setStatus("Employee no longer exists");
                        view.showError("Employee not found");
                        return;
                    }
                    enterEditMode(employee);
                    view.showEmployee(employee);
                    view.setStatus("Ready");
                }, error -> {
                    clearForm();
                    view.setStatus("Unable to load employee");
                    view.showError(error.getMessage());
                }, this::finishTask);
    }

    public void saveEmployee(Employee formEmployee) {
        if (busy) return;
        boolean creating = getMode() == CrudMode.CREATE;
        if (!creating) {
            formEmployee.setId(getSelectedEntity().getId());
            formEmployee.setUserId(getSelectedEntity().getUserId());
        }
        runMutation(() -> creating
                ? employeeService.create(formEmployee)
                : employeeService.update(formEmployee),
                "Saving employee...", "Unable to save employee");
    }

    public void clearForm() {
        enterCreateMode();
        view.clearSelection();
        view.clearEmployeeForm();
        updateActionState();
    }


    private void runMutation(Callable<Boolean> mutation, String status, String failureStatus) {
        if (busy) return;
        busy = true;
        view.setBusy(true);
        view.setStatus(status);
        boolean[] saved = {false};
        AsyncTaskRunner.run(mutation,
                success -> {
                    if (!success) {
                        view.setStatus(failureStatus);
                        view.showError(failureStatus);
                        return;
                    }
                    saved[0] = true;
                    clearForm();
                }, error -> {
                    view.setStatus(failureStatus);
                    view.showError(error.getMessage());
                }, () -> {
                    if (saved[0]) {
                        busy = false;
                        runTask(employeeService::findEmployeeListViews, "Refreshing employees...",
                                "Changes saved; unable to refresh the list. Click Refresh to reload.");
                    } else {
                        finishTask();
                    }
                });
    }

    private void runTask(Callable<List<EmployeeListViewDto>> task, String status, String failureStatus) {
        if (busy) return;
        busy = true;
        view.setBusy(true);
        view.setStatus(status);
        AsyncTaskRunner.run(task,
                employees -> {
                    clearForm();
                    view.setEmployees(employees);
                    view.setStatus("Ready");
                }, error -> {
                    view.setStatus(failureStatus);
                    view.showError(error.getMessage());
                }, this::finishTask);
    }

    private void finishTask() {
        busy = false;
        view.setBusy(false);
        updateActionState();
    }

    private void updateActionState() {
        view.setUpdateEnabled(hasSelection());
    }
}
