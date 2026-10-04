package com.storemanager.domain.system_tool.service;

import com.storemanager.config.DatabaseSettings;
import com.storemanager.core.database.ConnectionFactory;
import com.storemanager.core.database.DatabaseInitializer;
import com.storemanager.core.security.PermissionGuard;
import com.storemanager.domain.audit.service.AuditService;
import com.storemanager.domain.notification.model.NotificationType;
import com.storemanager.domain.notification.service.NotificationService;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class DatabaseRestoreService {

    private final AuditService auditService =
            new AuditService();

    private final NotificationService notificationService =
            new NotificationService();

    public boolean restore(
            String mysqlPath,
            File inputFile
    ) {

        if (!PermissionGuard.canRestoreDatabase()) {
            auditService.recordPermissionDenied("DATABASE_RESTORE", "RESTORE", null, "Restore access denied", "OWNER");
            throw new RuntimeException(
                    "Current user cannot restore database"
            );
        }

        if (mysqlPath == null || mysqlPath.trim().isEmpty()) {
            throw new RuntimeException(
                    "mysql path is required"
            );
        }

        if (inputFile == null || !inputFile.isFile()) {
            throw new RuntimeException(
                    "A complete backup ZIP or legacy SQL file is required"
            );
        }

        String name = inputFile.getName().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".zip") && !name.endsWith(".sql")) {
            throw new IllegalArgumentException("Select a complete backup .zip or legacy .sql file");
        }

        Path extracted = null;
        boolean success;
        try {
            File sqlFile = inputFile;
            CompleteBackupArchive archive = new CompleteBackupArchive();
            if (name.endsWith(".zip")) {
                extracted = Files.createTempDirectory("storemanager-restore-");
                // Reject malformed archives and unsafe paths before touching the database.
                sqlFile = archive.extract(inputFile.toPath(), extracted).toFile();
            }
            success = runSql(mysqlPath.trim(), sqlFile, ConnectionFactory.getCurrentSettings());
            if (success && extracted != null) {
                try {
                    archive.restoreImages(extracted.resolve("data/images"), Path.of("data", "images"));
                } catch (IOException e) {
                    throw new IOException("Database restored, but images could not be restored. Keep the backup and retry: "
                            + e.getMessage(), e);
                }
            }
            if (success) {
                try {
                    DatabaseInitializer.initialize();
                } catch (RuntimeException e) {
                    throw new IllegalStateException("Database restored, but schema initialization failed: "
                            + errorDetails(e), e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Database restore failed: " + e.getMessage(), e);
        } finally {
            CompleteBackupArchive.deleteTemporaryDirectory(extracted);
        }

        if (success) {
            auditService.recordEvent(
                    "DATABASE_TOOLS",
                    "DATABASE_RESTORE",
                    "DATABASE",
                    null,
                    true,
                    null,
                    "{\"path\":\"" + escape(inputFile.getAbsolutePath()) + "\"}"
            );

            notificationService.notifyCurrentUser(
                    "Restore completed",
                    "Database restored from " + inputFile.getAbsolutePath(),
                    NotificationType.SYSTEM
            );
        }

        return success;
    }

    private boolean runSql(String executable, File sqlFile, DatabaseSettings settings) {
        Path errorFile = null;
        Process process = null;
        try {
            errorFile = Files.createTempFile("storemanager-restore-", ".stderr");
            ProcessBuilder builder = new ProcessBuilder(executable,
                    "--host=" + settings.getHost(), "--port=" + settings.getPort(),
                    "--user=" + settings.getUsername(), settings.getDatabaseName());
            builder.environment().put("MYSQL_PWD", settings.getPassword() == null ? "" : settings.getPassword());
            builder.redirectInput(sqlFile);
            builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            builder.redirectError(errorFile.toFile());
            process = builder.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String error;
                try (var input = Files.newInputStream(errorFile)) {
                    error = new String(input.readNBytes(4096), StandardCharsets.UTF_8).trim();
                }
                throw new IOException("mysql exited with code " + exitCode + (error.isEmpty() ? "" : ": " + error));
            }
            return true;
        } catch (InterruptedException e) {
            if (process != null) process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new RuntimeException("Database restore interrupted", e);
        } catch (Exception e) {
            throw new RuntimeException("Database restore failed: " + e.getMessage(), e);
        } finally {
            if (errorFile != null) {
                try { Files.deleteIfExists(errorFile); } catch (IOException ignored) { /* Best effort. */ }
            }
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

    private String errorDetails(Throwable error) {
        String message = error.getMessage();
        Throwable cause = error.getCause();
        while (cause != null) {
            if (cause.getMessage() != null && !cause.getMessage().isBlank()) {
                message = cause.getMessage();
            }
            cause = cause.getCause();
        }
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }
}
