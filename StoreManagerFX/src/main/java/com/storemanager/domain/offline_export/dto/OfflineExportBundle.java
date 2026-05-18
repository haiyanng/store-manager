package com.storemanager.domain.offline_export.dto;

import java.util.List;

public record OfflineExportBundle(
        List<BranchSnapshot> branches,
        List<UserSnapshot> users,
        List<EmployeeSnapshot> employees,
        List<CategorySnapshot> categories,
        List<ProductSnapshot> products,
        List<InventorySnapshot> inventory,
        List<AttendanceSnapshot> attendance,
        List<PayrollSnapshot> payroll,
        List<SaleSnapshot> sales,
        List<SaleItemSnapshot> saleItems,
        List<AuditLogSnapshot> auditLogs,
        List<ExportImageAsset> imageAssets
) {
}
