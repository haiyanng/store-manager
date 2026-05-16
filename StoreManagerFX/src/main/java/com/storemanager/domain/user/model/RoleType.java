package com.storemanager.domain.user.model;

public enum RoleType {

    DEVELOPER,

    OWNER,

    MANAGER,

    EMPLOYEE;

    public static RoleType fromDatabaseValue(
            String value
    ) {

        if (value == null) {
            throw new IllegalArgumentException(
                    "Role value cannot be null"
            );
        }

        String normalizedValue =
                value.trim().toUpperCase();

        if ("ADMIN".equals(normalizedValue)) {
            return DEVELOPER;
        }

        return RoleType.valueOf(normalizedValue);
    }
}
