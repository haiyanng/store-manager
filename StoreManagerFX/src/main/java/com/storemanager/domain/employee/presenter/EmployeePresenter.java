package com.storemanager.domain.employee.presenter;

import com.storemanager.core.runtime.BaseCrudPresenter;
import com.storemanager.core.runtime.CrudMode;
import com.storemanager.domain.employee.model.Employee;
import com.storemanager.domain.employee.service.EmployeeService;
import com.storemanager.domain.employee.view.EmployeeListController;
import com.storemanager.domain.user.model.User;

import java.util.Map;

public class EmployeePresenter extends BaseCrudPresenter<Employee> {

    private final EmployeeListController view;

    private final EmployeeService employeeService =
            new EmployeeService();

    private Map<Long, User> usersById =
            Map.of();

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

        usersById =
                employeeService.findUsersById();

        view.setEmployees(
                employeeService.findAll()
        );

        updateActionState();
    }

    public void selectEmployee(
            Employee employee
    ) {

        if (employee == null) {
            enterCreateMode();
            view.clearEmployeeForm();
            updateActionState();
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

    public String getLinkedUsername(
            Employee employee
    ) {

        User user =
                getLinkedUser(employee);

        if (user == null) {
            return "";
        }

        return user.getUsername();
    }

    public String getLinkedRole(
            Employee employee
    ) {

        User user =
                getLinkedUser(employee);

        if (user == null) {
            return "";
        }

        return user.getRole().name();
    }

    private User getLinkedUser(
            Employee employee
    ) {

        if (employee == null || employee.getUserId() == null) {
            return null;
        }

        return usersById.get(
                employee.getUserId()
        );
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
