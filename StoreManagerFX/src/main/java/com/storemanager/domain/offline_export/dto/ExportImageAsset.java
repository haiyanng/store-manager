package com.storemanager.domain.offline_export.dto;

import java.io.File;

public record ExportImageAsset(
        String sourceType,
        Long sourceId,
        String sourcePath,
        String exportPath,
        File sourceFile
) {
}
