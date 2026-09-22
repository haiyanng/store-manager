package com.storemanager.domain.user.model;

public enum RoleType {

    CUSTOMER,

    OWNER,

    MANAGER,

    STAFF,

    VIEWER,

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
                value.trim().toUpperCase(java.util.Locale.ROOT);

        if ("USER".equals(normalizedValue)) {
            return CUSTOMER;
        }

        if ("EMPLOYEE".equals(normalizedValue)) {
            return STAFF;
        }

        return RoleType.valueOf(normalizedValue);
    }
}
