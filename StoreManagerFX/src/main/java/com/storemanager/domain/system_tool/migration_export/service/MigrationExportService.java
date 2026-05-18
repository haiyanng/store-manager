package com.storemanager.domain.system_tool.migration_export.service;

import com.storemanager.core.session.AppSession;
import com.storemanager.domain.offline_export.dto.OfflineExportSelection;
import com.storemanager.domain.offline_export.dto.OfflineExportResult;
import com.storemanager.domain.offline_export.service.OfflineExportService;
import com.storemanager.domain.system_tool.migration_export.model.MigrationDirection;
import com.storemanager.domain.system_tool.migration_export.model.MigrationHistoryEntry;
import com.storemanager.domain.system_tool.migration_export.model.MigrationPreviewResult;
import com.storemanager.domain.system_tool.migration_export.model.MigrationStatusLevel;
import com.storemanager.domain.user.model.User;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

public class MigrationExportService {

    private final OfflineExportService offlineExportService =
            new OfflineExportService();

    private final MigrationPackagePreviewService previewService =
            new MigrationPackagePreviewService();

    private final MigrationHistoryStore historyStore =
            new MigrationHistoryStore();

    public OfflineExportResult exportPackage(
            String businessName,
            File outputZipFile,
            OfflineExportSelection selection
    ) {

        return offlineExportService.exportBusinessSnapshot(
                businessName,
                outputZipFile,
                selection
        );
    }

    public MigrationPreviewResult previewPackage(
            File packageFile
    ) {

        return previewService.preview(packageFile);
    }

    public boolean validatePackage(
            MigrationPreviewResult previewResult
    ) {

        return previewResult != null
                && previewResult.getValidationErrors().isEmpty();
    }

    public void recordExportHistory(
            int recordCount,
            MigrationStatusLevel status
    ) {

        historyStore.append(
                buildHistoryEntry(
                        MigrationDirection.EXPORT,
                        recordCount,
                        status
                )
        );
    }

    public void recordImportHistory(
            int recordCount,
            MigrationStatusLevel status
    ) {

        historyStore.append(
                buildHistoryEntry(
                        MigrationDirection.IMPORT,
                        recordCount,
                        status
                )
        );
    }

    public List<MigrationHistoryEntry> findHistory() {

        return historyStore.loadAll();
    }

    private MigrationHistoryEntry buildHistoryEntry(
            MigrationDirection direction,
            int recordCount,
            MigrationStatusLevel status
    ) {

        MigrationHistoryEntry entry =
                new MigrationHistoryEntry();

        entry.setDate(LocalDateTime.now());
        entry.setDirection(direction);
        entry.setStatus(status);
        entry.setRecordCount(recordCount);

        User currentUser =
                AppSession.getCurrentUser();

        entry.setUser(
                currentUser == null
                        ? "System"
                        : currentUser.getUsername()
        );

        return entry;
    }
}
