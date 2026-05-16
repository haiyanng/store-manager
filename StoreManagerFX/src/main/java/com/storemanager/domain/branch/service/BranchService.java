package com.storemanager.domain.branch.service;

import com.storemanager.core.security.PermissionGuard;
import com.storemanager.core.session.AppSession;
import com.storemanager.domain.branch.model.Branch;
import com.storemanager.domain.branch.model.EmployeeBranchAssignment;
import com.storemanager.domain.branch.repository.BranchRepository;
import com.storemanager.domain.employee.model.Employee;
import com.storemanager.domain.employee.service.EmployeeService;
import com.storemanager.domain.user.model.User;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BranchService {

    private final BranchRepository branchRepository =
            new BranchRepository();

    private final EmployeeService employeeService =
            new EmployeeService();

    public List<Branch> findBranches() {

        validateBranchAccess();

        return branchRepository.findAllBranches();
    }

    public List<Employee> findEmployees() {

        validateBranchAccess();

        return employeeService.findAll();
    }

    public List<EmployeeBranchAssignment> findAssignments() {

        validateBranchAccess();

        return branchRepository.findAllAssignments();
    }

    public Map<Long, Branch> findBranchesById() {

        return findBranches()
                .stream()
                .collect(Collectors.toMap(Branch::getId, branch -> branch));
    }

    public Map<Long, Employee> findEmployeesById() {

        return findEmployees()
                .stream()
                .collect(Collectors.toMap(Employee::getId, employee -> employee));
    }

    public boolean saveBranch(Branch branch) {

        validateBranchAccess();
        validateBranch(branch);

        if (branch.getId() == null) {
            return branchRepository.saveBranch(branch);
        }

        return branchRepository.updateBranch(branch);
    }

    public boolean assignEmployee(
            Employee employee,
            Branch branch
    ) {

        validateBranchAccess();
        validateEmployee(employee);
        validateBranchForAssignment(branch);

        if (branchRepository.hasActiveAssignment(
                employee.getId(),
                branch.getId()
        )) {
            throw new RuntimeException(
                    "Employee is already assigned to this branch"
            );
        }

        EmployeeBranchAssignment assignment =
                new EmployeeBranchAssignment();

        assignment.setEmployeeId(employee.getId());
        assignment.setBranchId(branch.getId());
        assignment.setActive(true);
        assignment.setAssignedByUserId(getCurrentUserId());

        return branchRepository.saveAssignment(assignment);
    }

    public boolean deactivateAssignment(
            EmployeeBranchAssignment assignment
    ) {

        validateBranchAccess();

        if (assignment == null || assignment.getId() == null) {
            throw new RuntimeException("Assignment is required");
        }

        return branchRepository.deactivateAssignment(assignment);
    }

    public boolean isEmployeeAssignedToBranch(
            Long employeeId,
            Long branchId
    ) {

        if (employeeId == null || branchId == null) {
            return false;
        }

        return branchRepository.hasActiveAssignment(employeeId, branchId);
    }

    private void validateBranchAccess() {

        if (!PermissionGuard.canViewEmployee()) {
            throw new RuntimeException("Branch access denied");
        }
    }

    private void validateBranch(Branch branch) {

        if (branch == null) {
            throw new RuntimeException("Branch is required");
        }

        if (branch.getName() == null || branch.getName().trim().isEmpty()) {
            throw new RuntimeException("Branch name is required");
        }

        branch.setName(branch.getName().trim());
        branch.setAddress(clean(branch.getAddress()));
    }

    private void validateBranchForAssignment(Branch branch) {

        if (branch == null || branch.getId() == null) {
            throw new RuntimeException("Branch is required");
        }

        if (!branch.isActive()) {
            throw new RuntimeException("Branch is inactive");
        }
    }

    private void validateEmployee(Employee employee) {

        if (employee == null || employee.getId() == null) {
            throw new RuntimeException("Employee is required");
        }
    }

    private Long getCurrentUserId() {

        User currentUser =
                AppSession.getCurrentUser();

        return currentUser == null ? null : currentUser.getId();
    }

    private String clean(String value) {

        if (value == null) {
            return "";
        }

        return value.trim();
    }
}
