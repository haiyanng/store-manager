package com.storemanager.domain.system_tool.migration_export.presenter;

import com.storemanager.config.DatabaseSettings;
import com.storemanager.core.database.ConnectionFactory;
import com.storemanager.core.runtime.async.AsyncTaskRunner;
import com.storemanager.core.runtime.async.LoadingState;
import com.storemanager.domain.offline_export.dto.OfflineExportResult;
import com.storemanager.domain.offline_export.dto.OfflineExportSelection;
import com.storemanager.domain.system_tool.migration_export.model.MigrationImportResult;
import com.storemanager.domain.system_tool.migration_export.model.MigrationPreviewResult;
import com.storemanager.domain.system_tool.migration_export.model.MigrationStatusLevel;
import com.storemanager.domain.system_tool.migration_export.service.MigrationExportService;
import com.storemanager.domain.system_tool.migration_export.view.MigrationExportController;

import java.io.File;
import java.util.Map;

public class MigrationExportPresenter {

    private final MigrationExportController view;

    private final MigrationExportService migrationExportService =
            new MigrationExportService();

    private LoadingState loadingState =
            LoadingState.IDLE;

    private MigrationPreviewResult currentPreview;

    private File currentPackageFile;

    public MigrationExportPresenter(
            MigrationExportController view
    ) {

        this.view = view;
    }

    public void initialize() {

        loadingState =
                LoadingState.IDLE;
        view.setStatus("Ready");
        view.setHistory(
                migrationExportService.findHistory()
        );
        view.setPreview(null);
    }

    public void onPackageSelected(
            File packageFile
    ) {

        currentPreview = null;
        currentPackageFile = packageFile;
        view.setPreview(null);
        view.setStatus(
                packageFile == null
                        ? "No package selected"
                        : "Package selected"
        );
    }

    public void exportPackage(
            File outputFile,
            OfflineExportSelection selection
    ) {

        loadingState =
                LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus("Exporting package...");

        String businessName =
                resolveBusinessName();

        AsyncTaskRunner.run(
                () -> migrationExportService.exportPackage(
                        businessName,
                        outputFile,
                        selection
                ),
                result -> handleExportSuccess(
                        outputFile,
                        result
                ),
                throwable -> {
                    loadingState =
                            LoadingState.ERROR;
                    view.setStatus("Export failed");
                    view.showError(
                            throwable.getMessage()
                    );
                },
                () -> view.setBusy(false)
        );
    }

    public void previewPackage(
            File packageFile
    ) {

        loadingState =
                LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus("Previewing package...");

        AsyncTaskRunner.run(
                () -> migrationExportService.previewPackage(
                        packageFile
                ),
                result -> {
                    currentPreview = result;
                    view.setPreview(result);

                    if (result.getValidationErrors().isEmpty()) {
                        loadingState =
                                LoadingState.SUCCESS;
                        view.setStatus(
                                result.getWarnings().isEmpty()
                                        ? "Preview ready"
                                        : "Preview has warnings"
                        );
                    } else {
                        loadingState =
                                LoadingState.ERROR;
                        view.setStatus("Preview has errors");
                    }
                },
                throwable -> {
                    loadingState =
                            LoadingState.ERROR;
                    view.setStatus("Preview failed");
                    view.showError(
                            throwable.getMessage()
                    );
                },
                () -> view.setBusy(false)
        );
    }

    public void validateCurrentPackage() {

        if (currentPreview == null) {
            view.showWarning(
                    "Preview the selected package first."
            );
            return;
        }

        if (migrationExportService.validatePackage(currentPreview)) {
            loadingState =
                    LoadingState.SUCCESS;
            view.setStatus(
                    currentPreview.getWarnings().isEmpty()
                            ? "Package validated"
                            : "Package validated with warnings"
            );
            view.showInfo(
                    currentPreview.getWarnings().isEmpty()
                            ? "Package validated successfully."
                            : "Package validated with warnings."
            );
            return;
        }

        loadingState =
                LoadingState.ERROR;
        view.setStatus("Validation failed");
        view.showError("Package validation failed");
    }

    public void importPackage() {

        if (currentPreview == null || currentPackageFile == null) {
            view.showWarning(
                    "Preview and validate the package first."
            );
            return;
        }

        if (!migrationExportService.validatePackage(currentPreview)) {
            view.showError(
                    "Fix preview validation errors before import."
            );
            return;
        }

        loadingState =
                LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus("Importing package...");

        AsyncTaskRunner.run(
                () -> migrationExportService.importPackage(
                        currentPackageFile
                ),
                this::handleImportSuccess,
                throwable -> {
                    loadingState =
                            LoadingState.ERROR;
                    view.setStatus("Import failed");
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

    private void handleExportSuccess(
            File outputFile,
            OfflineExportResult result
    ) {

        loadingState =
                LoadingState.SUCCESS;
        migrationExportService.recordExportHistory(
                countRecords(result.manifest().recordCounts()),
                MigrationStatusLevel.SUCCESS
        );

        view.setHistory(
                migrationExportService.findHistory()
        );
        view.setStatus("Export completed");
        view.showInfo(
                "Export package created: "
                        + outputFile.getAbsolutePath()
        );
    }

    private String resolveBusinessName() {

        DatabaseSettings settings =
                ConnectionFactory.getCurrentSettings();

        if (settings == null
                || settings.getDatabaseName() == null
                || settings.getDatabaseName().trim().isEmpty()) {
            return "StoreManagerFX";
        }

        return settings.getDatabaseName().trim();
    }

    private void handleImportSuccess(
            MigrationImportResult result
    ) {

        loadingState =
                LoadingState.SUCCESS;
        migrationExportService.recordImportHistory(
                result.totalCount(),
                MigrationStatusLevel.SUCCESS
        );

        view.setHistory(
                migrationExportService.findHistory()
        );
        view.setStatus("Import completed");
        view.showInfo(
                "Imported "
                        + result.productCount()
                        + " products, "
                        + result.categoryCount()
                        + " categories, "
                        + result.inventoryCount()
                        + " inventory rows."
        );
    }

    private int countRecords(
            Map<String, Integer> counts
    ) {

        if (counts == null) {
            return 0;
        }

        return counts.values()
                .stream()
                .mapToInt(
                        value -> value == null ? 0 : value
                )
                .sum();
    }
}
