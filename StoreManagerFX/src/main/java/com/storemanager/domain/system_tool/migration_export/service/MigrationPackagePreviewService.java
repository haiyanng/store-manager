package com.storemanager.domain.system_tool.migration_export.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.storemanager.domain.offline_export.OfflineExportSpecV1;
import com.storemanager.domain.offline_export.dto.OfflineExportManifest;
import com.storemanager.domain.system_tool.migration_export.model.MigrationPreviewResult;

import java.io.File;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MigrationPackagePreviewService {

    private final ObjectMapper objectMapper =
            new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public MigrationPreviewResult preview(
            File packageFile
    ) {

        if (packageFile == null || !packageFile.isFile()) {
            throw new RuntimeException("Package file is required");
        }

        MigrationPreviewResult result =
                new MigrationPreviewResult();

        try (ZipFile zipFile = new ZipFile(packageFile)) {

            OfflineExportManifest manifest =
                    readManifest(zipFile)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "manifest.json is missing"
                                    )
                            );

            result.setExportVersion(
                    manifest.exportVersion()
            );
            result.setSourceSystem(
                    manifest.sourceSystem()
            );
            result.setAppVersion(
                    manifest.appVersion()
            );
            result.setBusinessName(
                    manifest.businessName()
            );
            result.setRecordCounts(
                    normalizeCounts(
                            manifest.recordCounts()
                    )
            );

            if (!OfflineExportSpecV1.EXPORT_VERSION.equals(
                    manifest.exportVersion()
            )) {
                result.getWarnings().add(
                        "Export version differs from the current foundation."
                );
            }

            validateRequiredEntries(
                    zipFile,
                    result
            );

            if (!OfflineExportSpecV1.SOURCE_SYSTEM.equals(
                    manifest.sourceSystem()
            )) {
                result.getWarnings().add(
                        "Source system is not the offline local system."
                );
            }

            return result;

        } catch (Exception e) {

            result.getValidationErrors().add(
                    e.getMessage() == null
                            ? "Package preview failed"
                            : e.getMessage()
            );

            return result;
        }
    }

    private Optional<OfflineExportManifest> readManifest(
            ZipFile zipFile
    ) throws Exception {

        ZipEntry manifestEntry =
                zipFile.getEntry(
                        OfflineExportSpecV1.MANIFEST_FILE
                );

        if (manifestEntry == null) {
            return Optional.empty();
        }

        try (
                InputStream inputStream =
                        zipFile.getInputStream(manifestEntry)
        ) {

            return Optional.of(
                    objectMapper.readValue(
                            inputStream,
                            OfflineExportManifest.class
                    )
            );
        }
    }

    private Map<String, Integer> normalizeCounts(
            Map<String, Integer> counts
    ) {

        if (counts == null) {
            return new LinkedHashMap<>();
        }

        return new LinkedHashMap<>(counts);
    }

    private void validateRequiredEntries(
            ZipFile zipFile,
            MigrationPreviewResult result
    ) {

        checkEntry(zipFile, OfflineExportSpecV1.USERS_FILE, result, false);
        checkEntry(zipFile, OfflineExportSpecV1.EMPLOYEES_FILE, result, false);
        checkEntry(zipFile, OfflineExportSpecV1.CATEGORIES_FILE, result, false);
        checkEntry(zipFile, OfflineExportSpecV1.PRODUCTS_FILE, result, false);
        checkEntry(zipFile, OfflineExportSpecV1.INVENTORY_FILE, result, false);
        checkEntry(zipFile, OfflineExportSpecV1.ATTENDANCE_FILE, result, false);
        checkEntry(zipFile, OfflineExportSpecV1.SALES_FILE, result, false);
        checkEntry(zipFile, OfflineExportSpecV1.SALE_ITEMS_FILE, result, false);
        checkEntry(zipFile, OfflineExportSpecV1.AUDIT_LOGS_FILE, result, false);
    }

    private void checkEntry(
            ZipFile zipFile,
            String name,
            MigrationPreviewResult result,
            boolean required
    ) {

        if (zipFile.getEntry(name) == null) {
            String message =
                    (required ? "Missing required entry: " : "Missing optional entry: ")
                            + name;

            if (required) {
                result.getValidationErrors().add(message);
            } else {
                result.getWarnings().add(message);
            }
        }
    }
}
