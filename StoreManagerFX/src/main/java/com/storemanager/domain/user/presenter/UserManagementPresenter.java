package com.storemanager.domain.user.presenter;

import com.storemanager.core.runtime.async.AsyncTaskRunner;
import com.storemanager.domain.employee.model.Employee;
import com.storemanager.domain.user.model.RoleType;
import com.storemanager.domain.user.model.User;
import com.storemanager.domain.user.service.UserManagementService;
import com.storemanager.domain.user.view.UserManagementController;
import java.util.List;
import java.util.concurrent.Callable;

public class UserManagementPresenter {
    private final UserManagementController view;
    private final UserManagementService service = new UserManagementService();
    private boolean busy;

    public UserManagementPresenter(UserManagementController view) {
        this.view = view;
    }

    public void refresh() { refresh("Ready"); }

    private void refresh(String completedMessage) {
        if (busy) return;
        setBusy(true);
        view.setStatus("Loading accounts...");
        AsyncTaskRunner.run(() -> new AccountData(service.findAll(), service.findEmployees()), data -> {
            view.setData(data.users(), data.employees());
            view.setStatus(completedMessage);
        }, error -> {
            view.setStatus("Unable to load accounts");
            view.showError(error.getMessage());
        }, () -> setBusy(false));
    }

    public void create(String username, String password, RoleType role, Employee employee) {
        mutate("Creating account...", "Account created", () -> {
            service.createUser(username, password, role, employee);
            return true;
        });
    }

    public void update(User user, String username, String password, RoleType role, Employee employee) {
        if (user == null) { view.showError("Select an account to edit"); return; }
        mutate("Saving account...", "Account saved", () -> service.updateUser(user, username, password, role, employee));
    }

    public void delete(User user) {
        if (user == null) { view.showError("Select an account to delete"); return; }
        mutate("Deleting account...", "Account deleted", () -> service.deleteUser(user));
    }

    private void mutate(String progress, String complete, Callable<Boolean> operation) {
        if (busy) return;
        setBusy(true);
        view.setStatus(progress);
        AsyncTaskRunner.run(operation, saved -> {
            setBusy(false);
            if (!saved) {
                view.setStatus("Account changes were not saved");
                view.showError("Unable to save account changes. Check the data and try again.");
                return;
            }
            view.clearForm();
            refresh("Ready. " + complete + ".");
        }, error -> {
            setBusy(false);
            String message = error.getMessage();
            view.setStatus("Account operation failed");
            view.showError(message);
            if (message != null && message.startsWith("Account was saved")) {
                view.clearForm();
                refresh("Ready. Account saved; check the employee link.");
            }
        }, null);
    }

    private void setBusy(boolean value) {
        busy = value;
        view.setBusy(value);
    }

    private record AccountData(List<User> users, List<Employee> employees) { }
}
