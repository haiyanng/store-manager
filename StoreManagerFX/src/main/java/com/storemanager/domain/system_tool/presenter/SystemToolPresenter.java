package com.storemanager.domain.system_tool.presenter;

import com.storemanager.core.runtime.async.AsyncTaskRunner;
import com.storemanager.core.runtime.async.LoadingState;
import com.storemanager.core.security.PermissionGuard;
import com.storemanager.domain.system_tool.service.DatabaseBackupService;
import com.storemanager.domain.system_tool.service.DatabaseRestoreService;
import com.storemanager.domain.system_tool.view.SystemToolController;
import com.storemanager.domain.system_tool.backup.model.BackupSummary;

import java.io.File;

public class SystemToolPresenter {

    private final SystemToolController view;

    private final DatabaseBackupService backupService =
            new DatabaseBackupService();

    private final DatabaseRestoreService restoreService =
            new DatabaseRestoreService();

    private LoadingState loadingState =
            LoadingState.IDLE;

    public SystemToolPresenter(
            SystemToolController view
    ) {

        this.view = view;
    }

    public void initialize() {

        if (!PermissionGuard.canAccessSystemTools()) {
            loadingState =
                    LoadingState.ERROR;
            view.setBusy(false);
            view.setStatus("Permission denied");
            view.showError("Bạn không có quyền truy cập System Tools");
            return;
        }

        loadingState =
                LoadingState.IDLE;
        view.setStatus("Ready");
        view.setBackupSummary(
                backupService.getLatestBackupSummary()
        );
    }

    public String createBackupFileName() {

        return backupService.createBackupFileName();
    }

    public void backup(
            String mysqldumpPath,
            File outputFile
    ) {

        loadingState =
                LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus("Backing up database...");

        AsyncTaskRunner.run(
                () -> backupService.backup(
                        mysqldumpPath,
                        outputFile
                ),
                success -> {
                    if (Boolean.TRUE.equals(success)) {
                        loadingState =
                                LoadingState.SUCCESS;
                        view.setBackupSummary(
                                backupService.getLatestBackupSummary()
                        );
                        String message =
                                "Backup created: "
                                        + outputFile.getAbsolutePath();
                        view.setStatus(message);
                        view.showInfo(message);
                    } else {
                        loadingState =
                                LoadingState.ERROR;
                        view.setStatus("Backup failed");
                        view.showError("Backup failed");
                    }
                },
                throwable -> {
                    loadingState =
                            LoadingState.ERROR;
                    view.setStatus("Backup failed");
                    view.showError(
                            throwable.getMessage()
                    );
                },
                () -> view.setBusy(false)
        );
    }

    public void restore(
            String mysqlPath,
            File inputFile
    ) {

        loadingState =
                LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus("Restoring database...");

        AsyncTaskRunner.run(
                () -> restoreService.restore(
                        mysqlPath,
                        inputFile
                ),
                success -> {
                    if (Boolean.TRUE.equals(success)) {
                        loadingState =
                                LoadingState.SUCCESS;
                        view.setStatus("Restore completed");
                        view.showInfo("Restore completed");
                    } else {
                        loadingState =
                                LoadingState.ERROR;
                        view.setStatus("Restore failed");
                        view.showError("Restore failed");
                    }
                },
                throwable -> {
                    loadingState =
                            LoadingState.ERROR;
                    view.setStatus("Restore failed");
                    view.showError(
                            throwable.getMessage()
                    );
                },
                () -> view.setBusy(false)
        );
    }

    public LoadingState getLoadingState() {
        return loadingState;
    }
}
