package com.storemanager.domain.offline_export.dto;

import java.io.File;

public record OfflineExportResult(
        File outputFile,
        OfflineExportManifest manifest
) {
}
