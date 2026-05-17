package com.storemanager.domain.system_tool.service;

import com.storemanager.config.DatabaseSettings;
import com.storemanager.core.database.ConnectionFactory;
import com.storemanager.core.security.PermissionGuard;
import com.storemanager.domain.audit.service.AuditService;
import com.storemanager.domain.notification.model.NotificationType;
import com.storemanager.domain.notification.service.NotificationService;

import java.io.File;

public class DatabaseRestoreService {

    private final AuditService auditService =
            new AuditService();

    private final NotificationService notificationService =
            new NotificationService();

    public boolean restore(
            String mysqlPath,
            File inputFile
    ) {

        if (!PermissionGuard.canAccessSystemTools()) {
            auditService.recordPermissionDenied(
                    AuditService.ACTION_PERMISSION_DENIED,
                    "RESTORE",
                    null,
                    "Restore access denied",
                    null
            );
            throw new RuntimeException(
                    "Current user cannot access system tools"
            );
        }

        if (mysqlPath == null || mysqlPath.trim().isEmpty()) {
            throw new RuntimeException(
                    "mysql path is required"
            );
        }

        if (inputFile == null || !inputFile.isFile()) {
            throw new RuntimeException(
                    "Restore SQL file is required"
            );
        }

        DatabaseSettings settings =
                ConnectionFactory.getCurrentSettings();

        ProcessBuilder processBuilder =
                new ProcessBuilder(
                        mysqlPath.trim(),
                        "--host=" + settings.getHost(),
                        "--port=" + settings.getPort(),
                        "--user=" + settings.getUsername(),
                        settings.getDatabaseName()
                );

        processBuilder.environment().put(
                "MYSQL_PWD",
                settings.getPassword() == null
                        ? ""
                        : settings.getPassword()
        );

        processBuilder.redirectInput(inputFile);
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(
                ProcessBuilder.Redirect.DISCARD
        );

        boolean success =
                run(processBuilder);

        if (success) {
            auditService.record(
                    AuditService.ACTION_RESTORE,
                    "DATABASE",
                    null,
                    "Database restored from " + inputFile.getAbsolutePath(),
                    null
            );

            notificationService.notifyCurrentUser(
                    "Restore completed",
                    "Database restored from " + inputFile.getAbsolutePath(),
                    NotificationType.SYSTEM
            );
        }

        return success;
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
                    "Database restore failed",
                    e
            );
        }
    }
}
