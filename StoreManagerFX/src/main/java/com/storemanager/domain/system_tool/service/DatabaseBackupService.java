package com.storemanager.domain.system_tool.service;

import com.storemanager.config.DatabaseSettings;
import com.storemanager.core.database.ConnectionFactory;
import com.storemanager.core.security.PermissionGuard;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DatabaseBackupService {

    private static final DateTimeFormatter FILE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public String createBackupFileName() {

        DatabaseSettings settings =
                ConnectionFactory.getCurrentSettings();

        return settings.getDatabaseName()
                + "_backup_"
                + LocalDateTime.now().format(FILE_TIMESTAMP)
                + ".sql";
    }

    public boolean backup(
            String mysqldumpPath,
            File outputFile
    ) {

        if (!PermissionGuard.canAccessSystemTools()) {
            throw new RuntimeException(
                    "Current user cannot access system tools"
            );
        }

        if (mysqldumpPath == null || mysqldumpPath.trim().isEmpty()) {
            throw new RuntimeException(
                    "mysqldump path is required"
            );
        }

        if (outputFile == null) {
            throw new RuntimeException(
                    "Backup output file is required"
            );
        }

        DatabaseSettings settings =
                ConnectionFactory.getCurrentSettings();

        ProcessBuilder processBuilder =
                new ProcessBuilder(
                        mysqldumpPath.trim(),
                        "--host=" + settings.getHost(),
                        "--port=" + settings.getPort(),
                        "--user=" + settings.getUsername(),
                        "--result-file=" + outputFile.getAbsolutePath(),
                        "--single-transaction",
                        "--routines",
                        "--triggers",
                        settings.getDatabaseName()
                );

        processBuilder.environment().put(
                "MYSQL_PWD",
                settings.getPassword() == null
                        ? ""
                        : settings.getPassword()
        );

        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(
                ProcessBuilder.Redirect.DISCARD
        );

        return run(processBuilder);
    }

    private boolean run(
            ProcessBuilder processBuilder
    ) {

        try {

            Process process =
                    processBuilder.start();

            return process.waitFor() == 0;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Database backup failed",
                    e
            );
        }
    }
}
