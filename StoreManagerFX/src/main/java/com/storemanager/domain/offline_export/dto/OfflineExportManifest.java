package com.storemanager.domain.offline_export.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public record OfflineExportManifest(
        String exportVersion,
        String sourceSystem,
        String appVersion,
        String businessName,
        OffsetDateTime exportedAt,
        Map<String, Integer> recordCounts
) {
}
