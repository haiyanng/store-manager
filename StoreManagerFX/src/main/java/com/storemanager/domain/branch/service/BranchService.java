package com.storemanager.domain.branch.service;

import com.storemanager.core.security.PermissionGuard;
import com.storemanager.core.session.AppSession;
import com.storemanager.domain.audit.service.AuditService;
import com.storemanager.domain.branch.model.Branch;
import com.storemanager.domain.branch.model.EmployeeBranchAssignment;
import com.storemanager.domain.branch.repository.BranchRepository;
import com.storemanager.domain.employee.model.Employee;
import com.storemanager.domain.employee.service.EmployeeService;
import com.storemanager.domain.notification.model.NotificationType;
import com.storemanager.domain.notification.service.NotificationService;
import com.storemanager.domain.user.model.User;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BranchService {

    private final BranchRepository branchRepository =
            new BranchRepository();

    private final EmployeeService employeeService =
            new EmployeeService();

    private final AuditService auditService =
            new AuditService();

    private final NotificationService notificationService =
            new NotificationService();

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

    public List<EmployeeBranchAssignment> findAssignmentsByEmployeeId(
            Long employeeId
    ) {

        validateBranchAccess();

        return branchRepository.findAllAssignments()
                .stream()
                .filter(assignment ->
                        employeeId != null
                                && employeeId.equals(assignment.getEmployeeId())
                )
                .toList();
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

    public List<Branch> findOperationalBranches() {

        User currentUser = requireCurrentUser();

        if (PermissionGuard.isDeveloper()
                || PermissionGuard.isOwner()) {
            return branchRepository.findActiveBranches();
        }

        Employee employee =
                employeeService.findByUserId(currentUser.getId());

        if (employee == null || employee.getId() == null) {
            return List.of();
        }

        return branchRepository.findActiveBranchesForEmployeeId(
                employee.getId()
        );
    }

    public List<Branch> findActiveBranchesForEmployeeId(
            Long employeeId
    ) {

        return branchRepository.findActiveBranchesForEmployeeId(
                employeeId
        );
    }

    public Branch resolveActiveBranchContext() {

        List<Branch> branches =
                findOperationalBranches();

        if (branches.isEmpty()) {
            return null;
        }

        Long activeBranchId =
                AppSession.getActiveBranchId();

        if (activeBranchId != null) {
            for (Branch branch : branches) {
                if (activeBranchId.equals(branch.getId())) {
                    return branch;
                }
            }
        }

        return branches.get(0);
    }

    public boolean canSwitchActiveBranch() {

        return PermissionGuard.isDeveloper()
                || PermissionGuard.isOwner();
    }

    public Branch findBranchById(
            Long branchId
    ) {

        return branchRepository.findBranchById(branchId);
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

        boolean saved =
                branchRepository.saveAssignment(assignment);

        if (saved) {
            auditService.record(
                    AuditService.ACTION_EMPLOYEE_BRANCH_ASSIGNMENT,
                    "EMPLOYEE_BRANCH_ASSIGNMENT",
                    assignment.getEmployeeId(),
                    "Assigned employee to branch",
                    branch.getId()
            );

            notifyEmployeeBranchAssignment(employee, branch);
        }

        return saved;
    }

    public boolean deactivateAssignment(
            EmployeeBranchAssignment assignment
    ) {

        validateBranchAccess();

        if (assignment == null || assignment.getId() == null) {
            throw new RuntimeException("Assignment is required");
        }

        boolean deactivated =
                branchRepository.deactivateAssignment(assignment);

        if (deactivated) {
            auditService.record(
                    AuditService.ACTION_EMPLOYEE_BRANCH_ASSIGNMENT,
                    "EMPLOYEE_BRANCH_ASSIGNMENT",
                    assignment.getEmployeeId(),
                    "Deactivated employee branch assignment",
                    assignment.getBranchId()
            );
        }

        return deactivated;
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
            auditService.recordPermissionDenied(
                    AuditService.ACTION_PERMISSION_DENIED,
                    "BRANCH",
                    null,
                    "Branch access denied",
                    null
            );
            throw new RuntimeException("Branch access denied");
        }
    }

    private User requireCurrentUser() {

        User currentUser =
                AppSession.getCurrentUser();

        if (currentUser == null) {
            throw new RuntimeException("User session is required");
        }

        return currentUser;
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

    private void notifyEmployeeBranchAssignment(
            Employee employee,
            Branch branch
    ) {

        if (employee == null
                || employee.getUserId() == null
                || branch == null) {
            return;
        }

        notificationService.notifyUser(
                employee.getUserId(),
                "Branch reassignment",
                "You were assigned to branch "
                        + branch.getName(),
                NotificationType.BRANCH
        );
    }

    private String clean(String value) {

        if (value == null) {
            return "";
        }

        return value.trim();
    }
}
