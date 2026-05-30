package com.storemanager.domain.system_tool.migration_export.model;

public record MigrationImportResult(
        int categoryCount,
        int productCount,
        int inventoryCount
) {

    public int totalCount() {
        return categoryCount + productCount + inventoryCount;
    }
}
