# StoreManagerFX refactoring report

Verified on 2026-09-19.

## Changes

1. Removed the DEVELOPER enum and its special access rules. OWNER uses the existing owner permissions. Legacy USER values map to CUSTOMER when read. Startup persistently migrates USER to CUSTOMER and DEVELOPER/ADMIN to ordinary OWNER accounts without changing IDs, passwords or active flags. Migration is idempotent. CUSTOMER has no back-office permissions.
2. Renamed inventory navigation, header and placeholder to **View Inventory**. Deleted the adjustment form, Apply/Clear buttons and their handlers. Preserved stock/history tables, sorting, Refresh and asynchronous JavaFX loading. The read-only presenter uses the existing BaseModulePresenter.
3. Preserved InventoryService, InventoryRepository, Sales and Import business code. The shared canAdjustInventory permission still supports imports and internal stock transactions; the inventory screen has no write actions. Removing that shared permission would break imports, sale cancellation and package imports.
4. Deleted salary/payroll models, repository, service, presenter, controller and FXML. Removed payroll navigation, dashboard sections and export dependencies. Removed the final cash-flow total that depended on payroll; retained the existing business cash-flow calculation from revenue and import costs instead of treating missing payroll cost as zero.
5. Removed creation of salary/payroll tables on new installations. Existing historical database tables are not dropped. Only the deprecated NotificationType.PAYROLL value remains for persistence compatibility with old notifications; it does not enable payroll functionality.
6. No product-expiry module or expiry fields exist in this branch. The user confirmed this, so no new module or schema was invented.

## Verification checklist

- [x] Final `mvn clean test`: BUILD SUCCESS; 210 production source files compiled; **10 tests, 0 failures, 0 errors, 0 skipped**.
- [x] Tested legacy role mapping, surviving role permissions, login/logout sessions, inactive users, incorrect passwords and idempotent migration using isolated H2 databases in MySQL mode.
- [x] Actual FXMLLoader loading of inventory, dashboard and migration export, including completion of asynchronous inventory/dashboard loading. No missing bindings after removal of controls.
- [x] Stock workflow regression checks cover import, sale, transaction rollback and stock restoration.
- [x] Export works without salary tables and creates products/attendance/sales/manifest files without payroll.json.
- [x] Static validation of all 30 FXML files: no missing controllers, event handlers, includes or literal Java FXML routes.
- [x] `git diff --check` passes.
- [x] `mvn clean test javafx:run` opened a responsive StoreManagerFX window. The smoke-test window was closed normally; Maven exited with BUILD SUCCESS.
- [ ] Live MySQL end-to-end verification remains unavailable: localhost:3306 refused connections, and startup displayed database setup. No migration was applied to the real database in this session. H2 tests do not establish complete MySQL compatibility.

Existing build warnings concern JDK 23 compiling target 21 without --release, JavaFX dependency metadata and classpath-based JavaFX test startup. Production build settings were preserved; only test-scoped JUnit/H2 dependencies were added.

## Preserved scope

No source changes to Sales, Import, Products, Categories, Online Orders, Customers, application configuration, CSS, app-config.json, CustomerShopFX, store-api, customer-web, customer-web-api or design-preview. Branch/attendance_anomaly changes only remove DEVELOPER checks. Attendance FXML only loses the payroll wording. Dashboard, audit and export edits are limited to obsolete role/payroll dependencies. This is not a claim of end-to-end coverage for every unrelated feature.

USER audit entity/action strings remain unchanged because they identify account records, not roles.

## Complete diff and file inventory

[REFACTORING_CHANGES.patch](REFACTORING_CHANGES.patch) contains complete unified diffs for all tracked source/config changes and the three new test files. Patch paths are relative to the Git root StoreManagerFX directory (the parent of the Maven project). Changes are already applied in the workspace. `git apply --reverse --check` validated the patch against the resulting files.

### Created

- `src/test/java/com/storemanager/refactor/InventoryAndFxmlTest.java`
- `src/test/java/com/storemanager/refactor/RoleAndNavigationTest.java`
- `src/test/java/com/storemanager/refactor/RoleMigrationTest.java`
- `REFACTORING_REPORT.md` - this report and file inventory.
- `REFACTORING_CHANGES.patch` - complete unified diff.

### Updated (38)

- `AGENTS.md`
- `pom.xml`
- `src/main/java/com/storemanager/core/database/DatabaseInitializer.java`
- `src/main/java/com/storemanager/core/security/PermissionGuard.java`
- `src/main/java/com/storemanager/domain/attendance_anomaly/presenter/AttendanceAnomalyPresenter.java`
- `src/main/java/com/storemanager/domain/attendance_anomaly/service/AttendanceAnomalyService.java`
- `src/main/java/com/storemanager/domain/audit/service/AuditService.java`
- `src/main/java/com/storemanager/domain/branch/service/BranchService.java`
- `src/main/java/com/storemanager/domain/dashboard/model/DashboardAnalyticsSnapshot.java`
- `src/main/java/com/storemanager/domain/dashboard/model/DashboardCashFlowSummary.java`
- `src/main/java/com/storemanager/domain/dashboard/model/DashboardMenuItem.java`
- `src/main/java/com/storemanager/domain/dashboard/model/DashboardMenuRegistry.java`
- `src/main/java/com/storemanager/domain/dashboard/model/EmployeeDashboardDto.java`
- `src/main/java/com/storemanager/domain/dashboard/model/OwnerDashboardDto.java`
- `src/main/java/com/storemanager/domain/dashboard/presenter/DashboardShellPresenter.java`
- `src/main/java/com/storemanager/domain/dashboard/service/DashboardAnalyticsService.java`
- `src/main/java/com/storemanager/domain/dashboard/view/DashboardHomeController.java`
- `src/main/java/com/storemanager/domain/dashboard/view/DashboardShellController.java`
- `src/main/java/com/storemanager/domain/inventory/presenter/InventoryPresenter.java`
- `src/main/java/com/storemanager/domain/inventory/view/InventoryListController.java`
- `src/main/java/com/storemanager/domain/notification/model/NotificationType.java`
- `src/main/java/com/storemanager/domain/offline_export/OfflineExportSpecV1.java`
- `src/main/java/com/storemanager/domain/offline_export/dto/OfflineExportBundle.java`
- `src/main/java/com/storemanager/domain/offline_export/dto/OfflineExportSelection.java`
- `src/main/java/com/storemanager/domain/offline_export/service/OfflineExportJsonSerializer.java`
- `src/main/java/com/storemanager/domain/offline_export/service/OfflineExportManifestGenerator.java`
- `src/main/java/com/storemanager/domain/offline_export/service/OfflineExportSnapshotCollector.java`
- `src/main/java/com/storemanager/domain/system_tool/migration_export/service/MigrationPackagePreviewService.java`
- `src/main/java/com/storemanager/domain/system_tool/migration_export/view/MigrationExportController.java`
- `src/main/java/com/storemanager/domain/user/model/RoleType.java`
- `src/main/java/com/storemanager/domain/user/repository/UserRepository.java`
- `src/main/java/com/storemanager/domain/user/service/UserManagementService.java`
- `src/main/java/com/storemanager/domain/user/view/UserManagementController.java`
- `src/main/resources/fxml/attendance/attendance.fxml`
- `src/main/resources/fxml/dashboard/dashboard-home.fxml`
- `src/main/resources/fxml/inventory/inventory-list.fxml`
- `src/main/resources/fxml/inventory/inventory-placeholder.fxml`
- `src/main/resources/fxml/system_tool/migration-export.fxml`

### Deleted (11)

- `src/main/java/com/storemanager/domain/dashboard/model/DashboardPayrollSummary.java`
- `src/main/java/com/storemanager/domain/inventory/view/InventoryAdjustmentFormController.java`
- `src/main/java/com/storemanager/domain/offline_export/dto/PayrollSnapshot.java`
- `src/main/java/com/storemanager/domain/payroll/model/EmployeeSalaryConfig.java`
- `src/main/java/com/storemanager/domain/payroll/model/PayrollRecord.java`
- `src/main/java/com/storemanager/domain/payroll/presenter/PayrollPresenter.java`
- `src/main/java/com/storemanager/domain/payroll/repository/PayrollRepository.java`
- `src/main/java/com/storemanager/domain/payroll/service/PayrollService.java`
- `src/main/java/com/storemanager/domain/payroll/view/PayrollController.java`
- `src/main/resources/fxml/inventory/inventory-adjustment-form.fxml`
- `src/main/resources/fxml/payroll/payroll.fxml`

## Repeat verification

```powershell
mvn clean test
mvn clean javafx:run
```

FXML tests require a JavaFX graphics environment. Data tests use in-memory H2, never app-config.json or the live MySQL database.
