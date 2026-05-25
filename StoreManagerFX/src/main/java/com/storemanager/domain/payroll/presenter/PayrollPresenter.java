package com.storemanager.domain.payroll.presenter;

import com.storemanager.core.runtime.BaseModulePresenter;
import com.storemanager.core.runtime.async.AsyncTaskRunner;
import com.storemanager.core.runtime.async.LoadingState;
import com.storemanager.domain.employee.model.Employee;
import com.storemanager.domain.payroll.model.EmployeeSalaryConfig;
import com.storemanager.domain.payroll.model.PayrollRecord;
import com.storemanager.domain.payroll.service.PayrollService;
import com.storemanager.domain.payroll.view.PayrollController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PayrollPresenter extends BaseModulePresenter {

    private final PayrollController view;

    private final PayrollService payrollService =
            new PayrollService();

    private Map<Long, Employee> employeesById =
            Map.of();

    private LoadingState loadingState =
            LoadingState.IDLE;

    public PayrollPresenter(
            PayrollController view
    ) {

        this.view = view;
    }

    @Override
    public void initialize() {

        view.setPayrollAdministrationVisible(
                payrollService.canManagePayroll()
        );

        view.setPeriod(
                payrollService.getCurrentYear(),
                payrollService.getCurrentMonth()
        );

        loadPayrollData();
    }

    public void loadPayrollData() {

        loadingState =
                LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus("Loading payroll...");

        AsyncTaskRunner.run(
                () -> new PayrollData(
                        payrollService.findEmployees(),
                        payrollService.findSalaryConfigs(),
                        payrollService.findPayrollRecords()
                ),
                data -> {
                    employeesById =
                            data.employees()
                                    .stream()
                                    .collect(
                                            Collectors.toMap(
                                                    Employee::getId,
                                                    employee -> employee
                                            )
                                    );
                    view.setEmployees(data.employees());
                    view.setSalaryConfigs(data.salaryConfigs());
                    view.setPayrollRecords(data.records());
                    loadingState =
                            LoadingState.SUCCESS;
                    view.setStatus("Ready");
                },
                throwable -> {
                    loadingState =
                            LoadingState.ERROR;
                    view.setStatus("Cannot load payroll");
                    view.showError(throwable.getMessage());
                },
                () -> view.setBusy(false)
        );
    }

    public void saveSalaryConfig(
            Employee employee,
            BigDecimal hourlyRate,
            boolean active
    ) {

        if (!payrollService.canManagePayroll()) {
            view.showError("Payroll management access denied");
            return;
        }

        loadingState =
                LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus("Saving salary config...");

        AsyncTaskRunner.run(
                () -> payrollService.saveSalaryConfig(
                        employee,
                        hourlyRate,
                        active
                ),
                success -> {
                    if (!success) {
                        view.showError("Cannot save salary config");
                        view.setStatus("Cannot save salary config");
                        view.setBusy(false);
                        return;
                    }

                    view.clearSalaryForm();
                    loadPayrollData();
                },
                throwable -> {
                    loadingState =
                            LoadingState.ERROR;
                    view.setStatus("Cannot save salary config");
                    view.showError(throwable.getMessage());
                    view.setBusy(false);
                },
                null
        );
    }

    public void generatePayroll(
            int year,
            int month
    ) {

        if (!payrollService.canManagePayroll()) {
            view.showError("Payroll generation access denied");
            return;
        }

        loadingState =
                LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus("Generating payroll...");

        AsyncTaskRunner.run(
                () -> payrollService.generatePayroll(
                        year,
                        month
                ),
                generatedCount -> {
                    view.setStatus(
                            "Generated payroll records: " + generatedCount
                    );
                    loadPayrollData();
                },
                throwable -> {
                    loadingState =
                            LoadingState.ERROR;
                    view.setStatus("Cannot generate payroll");
                    view.showError(throwable.getMessage());
                    view.setBusy(false);
                },
                null
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

    private record PayrollData(
            List<Employee> employees,
            List<EmployeeSalaryConfig> salaryConfigs,
            List<PayrollRecord> records
    ) {
    }
}
