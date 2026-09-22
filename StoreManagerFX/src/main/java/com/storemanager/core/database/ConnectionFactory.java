package com.storemanager.core.database;

import com.storemanager.config.ConfigService;
import com.storemanager.config.DatabaseSettings;

import java.sql.Connection;
import java.sql.DriverManager;

public class ConnectionFactory {

    private static DatabaseSettings currentSettings;

    public static void setSettings(
            DatabaseSettings settings
    ) {

        currentSettings = settings;
    }

    public static DatabaseSettings getCurrentSettings() {

        if (currentSettings == null) {

            currentSettings =
                    ConfigService
                            .loadDatabaseSettings()
                            .orElseThrow(
                                    () -> new RuntimeException(
                                            "Database settings not found"
                                    )
                            );
        }

        return currentSettings;
    }

    public static Connection getConnection()
            throws Exception {

        DatabaseSettings settings =
                getCurrentSettings();

        return DriverManager.getConnection(
                settings.buildDatabaseUrl(),
                settings.getUsername(),
                settings.getPassword()
        );
    }

    public static Connection getServerConnection(
            DatabaseSettings settings
    ) throws Exception {

        return DriverManager.getConnection(
                settings.buildServerUrl(),
                settings.getUsername(),
                settings.getPassword()
        );
    }

    public static boolean testConnection(
            DatabaseSettings settings
    ) {

        try (
                Connection ignored =
                        getServerConnection(settings)
        ) {

            return true;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }
}