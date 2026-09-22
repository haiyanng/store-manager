package com.storemanager.domain.branch.model;

public class Branch {

    private Long id;

    private String name;

    private String address;

    private boolean active;

    public Branch() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {

        if (id == null) {
            return name;
        }

        return id + " - " + name;
    }
}
