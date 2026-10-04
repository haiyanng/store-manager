package com.storemanager.domain.offline_export.dto;

import java.util.List;

public record OfflineExportBundle(

        List<UserSnapshot> users,
        List<EmployeeSnapshot> employees,
        List<CategorySnapshot> categories,
        List<ProductSnapshot> products,
        List<InventorySnapshot> inventory,
        List<AttendanceSnapshot> attendance,
        List<SaleSnapshot> sales,
        List<SaleItemSnapshot> saleItems,
        List<AuditLogSnapshot> auditLogs,
        List<ExportImageAsset> imageAssets,
        List<ImportReceiptSnapshot> importReceipts,
        List<ImportItemSnapshot> importItems,
        List<InventoryTransactionSnapshot> inventoryTransactions
) {
    public OfflineExportBundle(List<UserSnapshot> users, List<EmployeeSnapshot> employees,
                               List<CategorySnapshot> categories, List<ProductSnapshot> products,
                               List<InventorySnapshot> inventory, List<AttendanceSnapshot> attendance,
                               List<SaleSnapshot> sales, List<SaleItemSnapshot> saleItems,
                               List<AuditLogSnapshot> auditLogs, List<ExportImageAsset> imageAssets) {
        this(users, employees, categories, products, inventory, attendance, sales, saleItems,
                auditLogs, imageAssets, List.of(), List.of(), List.of());
    }
}
