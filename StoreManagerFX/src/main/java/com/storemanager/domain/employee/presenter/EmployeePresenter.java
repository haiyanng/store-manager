package com.storemanager.domain.employee.presenter;

import com.storemanager.core.runtime.BaseCrudPresenter;
import com.storemanager.core.runtime.CrudMode;
import com.storemanager.core.runtime.async.AsyncTaskRunner;
import com.storemanager.domain.employee.model.EmployeeListViewDto;
import com.storemanager.domain.employee.model.Employee;
import com.storemanager.domain.employee.service.EmployeeService;
import com.storemanager.domain.employee.view.EmployeeListController;

public class EmployeePresenter extends BaseCrudPresenter<Employee> {

    private final EmployeeListController view;

    private final EmployeeService employeeService =
            new EmployeeService();

    public EmployeePresenter(
            EmployeeListController view
    ) {

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
        view.setBusy(true);
        view.setStatus("Loading employees...");
        AsyncTaskRunner.run(employeeService::findEmployeeListViews,
                employees -> {
                    view.setEmployees(employees);
                    view.setStatus("Ready");
                    updateActionState();
                },
                error -> {
                    view.setStatus("Unable to load employees");
                    view.showError(error.getMessage());
                }, () -> view.setBusy(false));
    }

    public void selectEmployee(
            EmployeeListViewDto employeeRow
    ) {

        if (employeeRow == null || employeeRow.getEmployeeId() == null) {
            enterCreateMode();
            view.clearEmployeeForm();
            updateActionState();
            return;
        }

        Employee employee;
        try {
            employee = employeeService.findById(employeeRow.getEmployeeId());
        } catch (RuntimeException e) {
            clearForm();
            view.showError(e.getMessage());
            return;
        }

        if (employee == null) {
            view.showError("Employee not found");
            clearForm();
            return;
        }

        enterEditMode(
                employee
        );

        view.showEmployee(
                employee
        );

        updateActionState();
    }

    public void saveEmployee(
            Employee formEmployee
    ) {

        try {

            boolean success;

            if (getMode() == CrudMode.CREATE) {
                success =
                        employeeService.create(
                                formEmployee
                        );
            } else {
                formEmployee.setId(
                        getSelectedEntity().getId()
                );

                formEmployee.setUserId(
                        getSelectedEntity().getUserId()
                );

                success =
                        employeeService.update(
                                formEmployee
                        );
            }

            if (!success) {
                view.showError(
                        getMode() == CrudMode.CREATE
                                ? "Cannot create employee"
                                : "Cannot update employee"
                );
                return;
            }

            clearForm();
            refreshTable();

        } catch (Exception e) {

            view.showError(
                    e.getMessage()
            );
        }
    }

    public void deleteEmployee() {

        if (!hasSelection()) {
            view.showError(
                    "Select an employee to delete"
            );
            return;
        }

        try {

            boolean success =
                    employeeService.delete(
                            getSelectedEntity()
                    );

            if (!success) {
                view.showError(
                        "Cannot delete employee"
                );
                return;
            }

            clearForm();
            refreshTable();

        } catch (Exception e) {

            view.showError(
                    e.getMessage()
            );
        }
    }

    public void clearForm() {

        enterCreateMode();
        view.clearSelection();
        view.clearEmployeeForm();
        updateActionState();
    }

    private void updateActionState() {

        boolean hasSelection =
                hasSelection();

        view.setUpdateEnabled(
                hasSelection
        );

        view.setDeleteEnabled(
                hasSelection
        );
    }
}
