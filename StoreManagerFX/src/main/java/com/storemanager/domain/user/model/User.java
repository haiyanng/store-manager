package com.storemanager.domain.user.model;

import java.time.LocalDateTime;

public class User {

    private Long id;

    private String username;

    private String password;

    private RoleType role;

    private boolean active;

    private LocalDateTime createdAt;



    public User() {
    }

    public User(
            Long id,
            String username,
            String password,
            RoleType role,
            boolean active,
            LocalDateTime createdAt
    ) {

        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(
            Long id
    ) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(
            String username
    ) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(
            String password
    ) {
        this.password = password;
    }

    public RoleType getRole() {
        return role;
    }

    public void setRole(
            RoleType role
    ) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(
            boolean active
    ) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(
            LocalDateTime createdAt
    ) {
        this.createdAt = createdAt;
    }
}