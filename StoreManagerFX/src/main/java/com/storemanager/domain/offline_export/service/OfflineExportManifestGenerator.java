package com.storemanager.domain.offline_export.service;

import com.storemanager.domain.offline_export.OfflineExportSpecV1;
import com.storemanager.domain.offline_export.dto.OfflineExportBundle;
import com.storemanager.domain.offline_export.dto.OfflineExportManifest;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class OfflineExportManifestGenerator {

    public OfflineExportManifest generate(
            String businessName,
            String appVersion,
            OfflineExportBundle bundle
    ) {

        return new OfflineExportManifest(
                OfflineExportSpecV1.EXPORT_VERSION,
                OfflineExportSpecV1.SOURCE_SYSTEM,
                normalize(appVersion),
                normalizeBusinessName(businessName),
                OffsetDateTime.now(),
                buildRecordCounts(bundle)
        );
    }

    private Map<String, Integer> buildRecordCounts(
            OfflineExportBundle bundle
    ) {

        Map<String, Integer> recordCounts =
                new LinkedHashMap<>();

        recordCounts.put(
                OfflineExportSpecV1.BRANCHES_FILE,
                size(bundle.branches())
        );
        recordCounts.put(
                OfflineExportSpecV1.USERS_FILE,
                size(bundle.users())
        );
        recordCounts.put(
                OfflineExportSpecV1.EMPLOYEES_FILE,
                size(bundle.employees())
        );
        recordCounts.put(
                OfflineExportSpecV1.CATEGORIES_FILE,
                size(bundle.categories())
        );
        recordCounts.put(
                OfflineExportSpecV1.PRODUCTS_FILE,
                size(bundle.products())
        );
        recordCounts.put(
                OfflineExportSpecV1.INVENTORY_FILE,
                size(bundle.inventory())
        );
        recordCounts.put(
                OfflineExportSpecV1.ATTENDANCE_FILE,
                size(bundle.attendance())
        );
        recordCounts.put(
                OfflineExportSpecV1.PAYROLL_FILE,
                size(bundle.payroll())
        );
        recordCounts.put(
                OfflineExportSpecV1.SALES_FILE,
                size(bundle.sales())
        );
        recordCounts.put(
                OfflineExportSpecV1.SALE_ITEMS_FILE,
                size(bundle.saleItems())
        );
        recordCounts.put(
                OfflineExportSpecV1.AUDIT_LOGS_FILE,
                size(bundle.auditLogs())
        );

        return recordCounts;
    }

    private int size(
            java.util.List<?> values
    ) {

        return values == null ? 0 : values.size();
    }

    private String normalize(
            String value
    ) {

        if (value == null || value.trim().isEmpty()) {
            return "UNKNOWN";
        }

        return value.trim();
    }

    private String normalizeBusinessName(
            String value
    ) {

        if (value == null || value.trim().isEmpty()) {
            return "Unnamed Business";
        }

        return value.trim();
    }
}
