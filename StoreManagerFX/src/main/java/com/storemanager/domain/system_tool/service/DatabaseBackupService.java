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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseBackupService {

    private static final Logger LOGGER = Logger.getLogger(DatabaseBackupService.class.getName());

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
            auditService.recordPermissionDenied("DATABASE_BACKUP", "BACKUP", null, "Backup access denied", "OWNER");
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

        boolean success;
        try {
            success = runBackup(mysqldumpPath, outputFile, settings);
        } catch (RuntimeException e) {
            auditService.recordEvent("DATABASE_TOOLS", "DATABASE_BACKUP", "DATABASE", null,
                    false, e.getMessage(), null);
            throw e;
        }

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
            try {
                backupHistoryStore.append(summary);
            } catch (RuntimeException e) {
                // The SQL file is already complete; history failure must not report backup failure.
                LOGGER.log(Level.WARNING, "Backup saved, but its history could not be updated", e);
            }

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

    private boolean runBackup(String executable, File outputFile, DatabaseSettings settings) {
        Path temporarySql = null;
        Path errorFile = null;
        Process process = null;
        try {
            Path destination = outputFile.toPath().toAbsolutePath().normalize();
            temporarySql = Files.createTempFile(destination.getParent(), "storemanager-backup-", ".sql.part");
            errorFile = Files.createTempFile(destination.getParent(), "storemanager-backup-", ".stderr");
            ProcessBuilder builder = new ProcessBuilder(
                    executable.trim(),
                    "--host=" + settings.getHost(),
                    "--port=" + settings.getPort(),
                    "--user=" + settings.getUsername(),
                    "--result-file=" + temporarySql,
                    "--single-transaction", "--routines", "--triggers", settings.getDatabaseName());
            builder.environment().put("MYSQL_PWD", settings.getPassword() == null ? "" : settings.getPassword());
            builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            builder.redirectError(errorFile.toFile());
            process = builder.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String error;
                try (var input = Files.newInputStream(errorFile)) {
                    error = new String(input.readNBytes(4096), StandardCharsets.UTF_8).trim();
                }
                throw new IOException("mysqldump exited with code " + exitCode + (error.isEmpty() ? "" : ": " + error));
            }
            if (Files.size(temporarySql) == 0) {
                throw new IOException("mysqldump produced an empty backup");
            }
            // Keep an existing backup intact until mysqldump has completed successfully.
            try {
                Files.move(temporarySql, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporarySql, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (InterruptedException e) {
            if (process != null) {
                process.destroyForcibly();
            }
            Thread.currentThread().interrupt();
            throw new RuntimeException("Database backup interrupted", e);
        } catch (Exception e) {
            throw new RuntimeException("Database backup failed: " + e.getMessage(), e);
        } finally {
            deleteTemporaryFile(temporarySql);
            deleteTemporaryFile(errorFile);
        }
    }

    private void deleteTemporaryFile(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException | SecurityException e) {
            LOGGER.log(Level.WARNING, "Cannot remove temporary backup file: " + path, e);
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
