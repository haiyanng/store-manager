package com.storemanager.domain.dashboard.presenter;

import com.storemanager.core.session.AppSession;
import com.storemanager.domain.attendance.model.AttendanceRuntimeStatus;
import com.storemanager.domain.attendance.model.AttendanceSession;
import com.storemanager.domain.attendance.service.AttendanceService;
import com.storemanager.domain.branch.model.Branch;
import com.storemanager.domain.branch.service.BranchService;
import com.storemanager.domain.dashboard.model.DashboardMenuItem;
import com.storemanager.domain.dashboard.model.DashboardMenuRegistry;
import com.storemanager.domain.employee.model.Employee;
import com.storemanager.domain.employee.service.EmployeeService;
import com.storemanager.domain.user.model.RoleType;
import com.storemanager.domain.user.model.User;

import java.util.List;
import java.util.Optional;

public class DashboardShellPresenter {

    private final BranchService branchService =
            new BranchService();

    private final EmployeeService employeeService =
            new EmployeeService();

    private final AttendanceService attendanceService =
            new AttendanceService();

    public List<DashboardMenuItem> getVisibleMenuItems() {

        RoleType role =
                getCurrentRole();

        return DashboardMenuRegistry.all()
                .stream()
                .filter(DashboardMenuItem::sidebarItem)
                .filter(menuItem -> menuItem.isVisibleFor(role))
                .toList();
    }

    public boolean canAccessMenuItem(
            String menuItemId
    ) {

        return findAccessibleMenuItem(menuItemId).isPresent();
    }

    public Optional<DashboardMenuItem> findAccessibleMenuItem(
            String menuItemId
    ) {

        if (menuItemId == null || menuItemId.isBlank()) {
            return Optional.empty();
        }

        RoleType role =
                getCurrentRole();

        return DashboardMenuRegistry.all()
                .stream()
                .filter(menuItem -> menuItem.id().equals(menuItemId))
                .filter(menuItem -> menuItem.isVisibleFor(role))
                .findFirst();
    }

    public BranchSelectorState resolveBranchSelectorState() {

        RoleType role =
                getCurrentRole();

        if (role == null) {
            return new BranchSelectorState(
                    List.of(),
                    null,
                    false,
                    false
            );
        }

        if (role == RoleType.DEVELOPER
                || role == RoleType.OWNER) {
            List<Branch> branches =
                    branchService.findBranches()
                            .stream()
                            .filter(Branch::isActive)
                            .toList();

            return new BranchSelectorState(
                    branches,
                    resolveSelectedBranch(branches),
                    !branches.isEmpty(),
                    true
            );
        }

        if (role == RoleType.MANAGER) {
            List<Branch> branches =
                    resolveAssignedBranchesForCurrentEmployee();

            return new BranchSelectorState(
                    branches,
                    resolveSelectedBranch(branches),
                    false,
                    false
            );
        }

        List<Branch> branches =
                resolveEmployeeBranchOptions();

        return new BranchSelectorState(
                branches,
                resolveInitialActiveBranchForCurrentUser(),
                false,
                false
        );
    }

    public boolean canSwitchBranch(
            Branch branch
    ) {

        if (branch == null || branch.getId() == null) {
            return false;
        }

        RoleType role =
                getCurrentRole();

        if (role != RoleType.DEVELOPER
                && role != RoleType.OWNER) {
            return false;
        }

        BranchSelectorState state =
                resolveBranchSelectorState();

        return state.branches().stream()
                .anyMatch(allowedBranch ->
                        branch.getId().equals(allowedBranch.getId())
                );
    }

    public void setActiveBranch(
            Branch branch
    ) {

        if (branch == null) {
            AppSession.setActiveBranch(null, null);
            return;
        }

        AppSession.setActiveBranch(
                branch.getId(),
                branch.getName()
        );
    }

    private RoleType getCurrentRole() {

        User currentUser =
                AppSession.getCurrentUser();

        return currentUser == null
                ? null
                : currentUser.getRole();
    }

    private Employee getCurrentEmployee() {

        User currentUser =
                AppSession.getCurrentUser();

        if (currentUser == null) {
            return null;
        }

        return employeeService.findByUserId(currentUser.getId());
    }

    private Branch resolveSelectedBranch(
            List<Branch> branches
    ) {

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

    public Branch resolveInitialActiveBranchForCurrentUser() {

        RoleType role =
                getCurrentRole();

        if (role == RoleType.DEVELOPER
                || role == RoleType.OWNER) {
            List<Branch> activeBranches =
                    branchService.findBranches()
                            .stream()
                            .filter(Branch::isActive)
                            .toList();
            return resolveSelectedBranch(activeBranches);
        }

        if (role == RoleType.MANAGER) {
            return resolveSelectedBranch(
                    resolveAssignedBranchesForCurrentEmployee()
            );
        }

        return resolveEmployeeBranchContext();
    }

    private Branch resolveEmployeeBranchContext() {

        Employee currentEmployee =
                getCurrentEmployee();

        if (currentEmployee == null) {
            return null;
        }

        AttendanceRuntimeStatus runtimeStatus =
                attendanceService.getCurrentRuntimeStatus();

        AttendanceSession activeSession =
                runtimeStatus.getActiveSession();

        if (activeSession != null
                && activeSession.getBranchId() != null) {
            Branch sessionBranch =
                    branchService.findBranchById(
                            activeSession.getBranchId()
                    );

            if (sessionBranch != null) {
                return sessionBranch;
            }

            Branch fallbackBranch =
                    new Branch();
            fallbackBranch.setId(activeSession.getBranchId());

            String activeBranchName =
                    runtimeStatus.getActiveBranchName();
            if (activeBranchName != null
                    && !activeBranchName.isBlank()
                    && !"-".equals(activeBranchName)) {
                fallbackBranch.setName(activeBranchName);
            } else {
                fallbackBranch.setName(
                        "Branch #" + activeSession.getBranchId()
                );
            }

            return fallbackBranch;
        }

        List<Branch> assignedBranches =
                resolveAssignedBranchesForCurrentEmployee();

        if (!assignedBranches.isEmpty()) {
            return resolveSelectedBranch(assignedBranches);
        }

        return null;
    }

    private List<Branch> resolveAssignedBranchesForCurrentEmployee() {

        Employee currentEmployee =
                getCurrentEmployee();

        if (currentEmployee == null) {
            return List.of();
        }

        return branchService.findActiveBranchesForEmployeeId(
                currentEmployee.getId()
        );
    }

    private List<Branch> resolveEmployeeBranchOptions() {

        Branch currentBranch =
                resolveEmployeeBranchContext();

        if (currentBranch == null) {
            return List.of();
        }

        return List.of(currentBranch);
    }

    public record BranchSelectorState(
            List<Branch> branches,
            Branch selectedBranch,
            boolean visible,
            boolean enabled
    ) {
    }
}
