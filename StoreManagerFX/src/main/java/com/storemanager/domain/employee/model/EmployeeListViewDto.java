package com.storemanager.domain.employee.model;

public class EmployeeListViewDto {

    private Long employeeId;

    private String fullName;

    private String phone;

    private String position;

    private boolean active;

    private String linkedUsername;

    private String linkedRole;

    private String branchDisplayName;

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getLinkedUsername() {
        return linkedUsername;
    }

    public void setLinkedUsername(String linkedUsername) {
        this.linkedUsername = linkedUsername;
    }

    public String getLinkedRole() {
        return linkedRole;
    }

    public void setLinkedRole(String linkedRole) {
        this.linkedRole = linkedRole;
    }

    public String getBranchDisplayName() {
        return branchDisplayName;
    }

    public void setBranchDisplayName(String branchDisplayName) {
        this.branchDisplayName = branchDisplayName;
    }
}
