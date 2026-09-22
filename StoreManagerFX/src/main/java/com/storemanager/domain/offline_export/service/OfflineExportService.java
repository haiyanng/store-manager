package com.storemanager.domain.offline_export.service;

import com.storemanager.core.security.PermissionGuard;
import com.storemanager.domain.offline_export.OfflineExportSpecV1;
import com.storemanager.domain.offline_export.dto.ExportImageAsset;
import com.storemanager.domain.offline_export.dto.OfflineExportBundle;
import com.storemanager.domain.offline_export.dto.OfflineExportManifest;
import com.storemanager.domain.offline_export.dto.OfflineExportSelection;
import com.storemanager.domain.offline_export.dto.OfflineExportResult;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public class OfflineExportService {

    private final OfflineExportSnapshotCollector snapshotCollector =
            new OfflineExportSnapshotCollector();

    private final OfflineExportJsonSerializer jsonSerializer =
            new OfflineExportJsonSerializer();

    private final OfflineExportManifestGenerator manifestGenerator =
            new OfflineExportManifestGenerator();

    private final OfflineExportZipPackageBuilder zipPackageBuilder =
            new OfflineExportZipPackageBuilder();

    public OfflineExportResult exportBusinessSnapshot(
            String businessName,
            File outputZipFile
    ) {

        return exportBusinessSnapshot(
                businessName,
                outputZipFile,
                OfflineExportSelection.all()
        );
    }

    public OfflineExportResult exportBusinessSnapshot(
            String businessName,
            File outputZipFile,
            OfflineExportSelection selection
    ) {

        validateAccess();
        validateOutputFile(outputZipFile);

        OfflineExportBundle bundle =
                snapshotCollector.collect(
                        selection
                );

        Path exportRoot = null;

        try {

            exportRoot =
                    Files.createTempDirectory(
                            "offline-export-"
                    );

            createImagesDirectory(exportRoot);

            jsonSerializer.writeSnapshots(
                    exportRoot,
                    bundle
            );

            copyImages(
                    exportRoot,
                    bundle
            );

            OfflineExportManifest manifest =
                    manifestGenerator.generate(
                            businessName,
                            resolveAppVersion(),
                            bundle
                    );

            jsonSerializer.writeManifest(
                    exportRoot,
                    manifest
            );

            zipPackageBuilder.build(
                    exportRoot,
                    outputZipFile.toPath()
            );

            return new OfflineExportResult(
                    outputZipFile,
                    manifest
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Cannot export offline business snapshot",
                    e
            );

        } finally {

            deleteRecursively(exportRoot);
        }
    }

    private void validateAccess() {

        if (!PermissionGuard.canAccessSystemTools()) {
            throw new RuntimeException(
                    "Current user cannot access system tools"
            );
        }
    }

    private void validateOutputFile(
            File outputZipFile
    ) {

        if (outputZipFile == null) {
            throw new RuntimeException(
                    "Export output file is required"
            );
        }
    }

    private void createImagesDirectory(
            Path exportRoot
    ) throws Exception {

        Files.createDirectories(
                exportRoot.resolve(
                        OfflineExportSpecV1.IMAGES_DIRECTORY
                )
        );
    }

    private void copyImages(
            Path exportRoot,
            OfflineExportBundle bundle
    ) throws Exception {

        for (ExportImageAsset imageAsset : bundle.imageAssets()) {

            if (imageAsset == null
                    || imageAsset.sourceFile() == null
                    || !imageAsset.sourceFile().isFile()) {
                continue;
            }

            Path target =
                    exportRoot.resolve(
                            imageAsset.exportPath()
                    );

            Files.createDirectories(target.getParent());
            Files.copy(
                    imageAsset.sourceFile().toPath(),
                    target,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

    private String resolveAppVersion() {

        Package packageMetadata =
                getClass().getPackage();

        if (packageMetadata != null
                && packageMetadata.getImplementationVersion() != null
                && !packageMetadata.getImplementationVersion().trim().isEmpty()) {
            return packageMetadata.getImplementationVersion().trim();
        }

        return "1.0-SNAPSHOT";
    }

    private void deleteRecursively(
            Path root
    ) {

        if (root == null) {
            return;
        }

        try {

            try (Stream<Path> paths = Files.walk(root)) {
                paths.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (Exception ignored) {
                                // Best-effort cleanup.
                            }
                        });
            }

        } catch (Exception ignored) {
            // Best-effort cleanup.
        }
    }
}
