package com.storemanager.core.database;

import java.sql.Connection;
import java.sql.Statement;
import com.storemanager.core.security.PasswordHasher;
import com.storemanager.domain.user.model.RoleType;
import com.storemanager.domain.user.model.User;
import com.storemanager.domain.user.repository.UserRepository;



public class DatabaseInitializer {

    public static void initialize() {

        try (
                Connection connection =
                        ConnectionFactory.getConnection();

                Statement statement =
                        connection.createStatement()
        ) {

            statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS users (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        username VARCHAR(100) NOT NULL UNIQUE,
                        password VARCHAR(255) NOT NULL,
                        role VARCHAR(50) NOT NULL,
                        active BOOLEAN NOT NULL DEFAULT TRUE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """
            );
            createDefaultAdmin();
            System.out.println("Database initialized successfully");

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Database initialization failed",
                    e
            );
        }
    }

    private static void createDefaultAdmin() {

        UserRepository repository =
                new UserRepository();

        boolean exists =
                repository
                        .findByUsername("admin")
                        .isPresent();

        if (exists) {
            return;
        }

        User admin =
                new User();

        admin.setUsername("admin");

        admin.setPassword(
                PasswordHasher.hash("123456")
        );

        admin.setRole(
                RoleType.DEVELOPER
        );

        admin.setActive(true);

        repository.save(admin);

        System.out.println(
                "Default admin created"
        );
    }
}