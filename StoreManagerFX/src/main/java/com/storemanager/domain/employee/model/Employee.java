package com.storemanager.domain.employee.model;

public class Employee {

    private Long id;

    private String fullName;

    private String phone;

    private String address;

    private String position;

    private boolean active;

    private Long userId;

    public Employee() {
    }

    public Long getId() {
        return id;
    }

    public void setId(
            Long id
    ) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(
            String fullName
    ) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(
            String phone
    ) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(
            String address
    ) {
        this.address = address;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(
            String position
    ) {
        this.position = position;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(
            boolean active
    ) {
        this.active = active;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(
            Long userId
    ) {
        this.userId = userId;
    }

    @Override
    public String toString() {

        if (id == null) {
            return fullName;
        }

        return id + " - " + fullName;
    }
}
