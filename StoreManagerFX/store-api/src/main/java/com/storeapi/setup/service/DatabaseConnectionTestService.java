package com.storeapi.setup.service;

import com.storeapi.setup.model.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseConnectionTestService {
    public boolean testConnection(DatabaseConfig config, String plainPassword) throws SQLException {
        DriverManager.setLoginTimeout(5);
        String password = plainPassword == null ? "" : plainPassword;
        try (Connection connection = DriverManager.getConnection(config.jdbcUrl(), config.username(), password)) {
            return connection.isValid(3) && hasCustomerSchema(connection);
        }
    }

    private boolean hasCustomerSchema(Connection connection) throws SQLException {
        return hasColumn(connection, "customers", "id")
                && hasColumn(connection, "customers", "email")
                && hasColumn(connection, "customers", "password_hash")
                && hasColumn(connection, "customers", "full_name")
                && hasColumn(connection, "customers", "phone")
                && hasColumn(connection, "customers", "active");
    }

    private boolean hasColumn(Connection connection, String table, String column) throws SQLException {
        try (ResultSet rs = connection.getMetaData().getColumns(connection.getCatalog(), null, table, column)) {
            return rs.next();
        }
    }
}
