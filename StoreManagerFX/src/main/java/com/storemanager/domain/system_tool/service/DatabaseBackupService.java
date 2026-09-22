package com.storemanager.domain.system_tool.service;

import com.storemanager.config.DatabaseSettings;
import com.storemanager.core.database.ConnectionFactory;
import com.storemanager.core.security.PermissionGuard;
import com.storemanager.domain.audit.service.AuditService;
import com.storemanager.domain.notification.model.NotificationType;
import com.storemanager.domain.notification.service.NotificationService;
import com.storemanager.domain.system_tool.backup.model.BackupSummary;
import com.storemanager.domain.system_tool.backup.service.BackupHistoryStore;
import com.storemanager.core.util.TimeFormatUtil;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DatabaseBackupService {

    private static final DateTimeFormatter FILE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final AuditService auditService =
            new AuditService();

    private final NotificationService notificationService =
            new NotificationService();

    private final BackupHistoryStore backupHistoryStore =
            new BackupHistoryStore();

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

        if (!PermissionGuard.canBackupDatabase()) {
            auditService.recordPermissionDenied(
                    "DATABASE_BACKUP",
                    "BACKUP",
                    null,
                    "Backup access denied",
                    null,
                    "OWNER"
            );
            throw new RuntimeException(
                    "Current user cannot backup database"
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

        boolean success =
                run(processBuilder);

        if (success) {

            BackupSummary summary =
                    new BackupSummary();
            summary.setCreatedAt(
                    TimeFormatUtil.truncateToSeconds(
                            LocalDateTime.now()
                    )
            );
            summary.setSizeBytes(outputFile.length());
            summary.setLocation(outputFile.getAbsolutePath());
            backupHistoryStore.append(summary);

            auditService.recordEvent(
                    "DATABASE_TOOLS",
                    "DATABASE_BACKUP",
                    "DATABASE",
                    null,
                    true,
                    null,
                    "{\"path\":\"" + escape(outputFile.getAbsolutePath()) + "\",\"size_bytes\":" + outputFile.length() + "}"
            );

            notificationService.notifyCurrentUser(
                    "Backup completed",
                    "Backup created at "
                            + outputFile.getAbsolutePath(),
                    NotificationType.SYSTEM
            );
        }

        return success;
    }

    public BackupSummary getLatestBackupSummary() {

        return backupHistoryStore.findLatest();
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

    private String escape(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
