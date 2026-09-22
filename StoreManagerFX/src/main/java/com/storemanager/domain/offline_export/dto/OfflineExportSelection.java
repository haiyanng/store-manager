package com.storemanager.domain.offline_export.dto;

public record OfflineExportSelection(
        boolean products,
        boolean employees,
        boolean sales,
        boolean attendance,
        boolean images,
        boolean auditLogs
) {

    public static OfflineExportSelection all() {

        return new OfflineExportSelection(
                true,
                true,
                true,
                true,
                true,
                true
        );
    }
}
