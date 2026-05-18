package com.storemanager.domain.offline_export.service;

import com.storemanager.core.storage.ImageStorageService;
import com.storemanager.domain.audit.model.AuditLogViewDto;
import com.storemanager.domain.audit.repository.AuditLogRepository;
import com.storemanager.domain.attendance.model.AttendanceSession;
import com.storemanager.domain.attendance.repository.AttendanceRepository;
import com.storemanager.domain.branch.model.Branch;
import com.storemanager.domain.branch.repository.BranchRepository;
import com.storemanager.domain.category.model.Category;
import com.storemanager.domain.category.repository.CategoryRepository;
import com.storemanager.domain.employee.model.Employee;
import com.storemanager.domain.employee.repository.EmployeeRepository;
import com.storemanager.domain.inventory.model.InventoryItem;
import com.storemanager.domain.inventory.repository.InventoryRepository;
import com.storemanager.domain.offline_export.OfflineExportSpecV1;
import com.storemanager.domain.offline_export.dto.AttendanceSnapshot;
import com.storemanager.domain.offline_export.dto.AuditLogSnapshot;
import com.storemanager.domain.offline_export.dto.BranchSnapshot;
import com.storemanager.domain.offline_export.dto.CategorySnapshot;
import com.storemanager.domain.offline_export.dto.EmployeeSnapshot;
import com.storemanager.domain.offline_export.dto.ExportImageAsset;
import com.storemanager.domain.offline_export.dto.InventorySnapshot;
import com.storemanager.domain.offline_export.dto.OfflineExportBundle;
import com.storemanager.domain.offline_export.dto.OfflineExportSelection;
import com.storemanager.domain.offline_export.dto.PayrollSnapshot;
import com.storemanager.domain.offline_export.dto.ProductSnapshot;
import com.storemanager.domain.offline_export.dto.SaleItemSnapshot;
import com.storemanager.domain.offline_export.dto.SaleSnapshot;
import com.storemanager.domain.offline_export.dto.UserSnapshot;
import com.storemanager.domain.payroll.model.PayrollRecord;
import com.storemanager.domain.payroll.repository.PayrollRepository;
import com.storemanager.domain.product.model.Product;
import com.storemanager.domain.product.repository.ProductRepository;
import com.storemanager.domain.sale.model.SaleOrder;
import com.storemanager.domain.sale.model.SaleOrderItem;
import com.storemanager.domain.sale.repository.SaleRepository;
import com.storemanager.domain.user.model.User;
import com.storemanager.domain.user.repository.UserRepository;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class OfflineExportSnapshotCollector {

    private final BranchRepository branchRepository =
            new BranchRepository();

    private final UserRepository userRepository =
            new UserRepository();

    private final EmployeeRepository employeeRepository =
            new EmployeeRepository();

    private final CategoryRepository categoryRepository =
            new CategoryRepository();

    private final ProductRepository productRepository =
            new ProductRepository();

    private final InventoryRepository inventoryRepository =
            new InventoryRepository();

    private final AttendanceRepository attendanceRepository =
            new AttendanceRepository();

    private final PayrollRepository payrollRepository =
            new PayrollRepository();

    private final SaleRepository saleRepository =
            new SaleRepository();

    private final AuditLogRepository auditLogRepository =
            new AuditLogRepository();

    private final ImageStorageService imageStorageService =
            new ImageStorageService();

    public OfflineExportBundle collect() {

        return collect(
                OfflineExportSelection.all()
        );
    }

    public OfflineExportBundle collect(
            OfflineExportSelection selection
    ) {

        List<ExportImageAsset> imageAssets =
                new ArrayList<>();

        List<BranchSnapshot> branches =
                branchRepository.findAllBranches()
                        .stream()
                        .map(this::toBranchSnapshot)
                        .toList();

        List<UserSnapshot> users =
                userRepository.findAll()
                        .stream()
                        .map(this::toUserSnapshot)
                        .toList();

        List<EmployeeSnapshot> employees =
                selection != null && selection.employees()
                        ? employeeRepository.findAll()
                                .stream()
                                .map(employee ->
                                        toEmployeeSnapshot(
                                                employee,
                                                selection.images(),
                                                imageAssets
                                        )
                                )
                                .toList()
                        : List.of();

        List<CategorySnapshot> categories =
                selection != null && selection.products()
                        ? categoryRepository.findAll()
                                .stream()
                                .map(category ->
                                        toCategorySnapshot(
                                                category,
                                                selection.images(),
                                                imageAssets
                                        )
                                )
                                .toList()
                        : List.of();

        List<ProductSnapshot> products =
                selection != null && selection.products()
                        ? productRepository.findAll()
                                .stream()
                                .map(product ->
                                        toProductSnapshot(
                                                product,
                                                selection.images(),
                                                imageAssets
                                        )
                                )
                                .toList()
                        : List.of();

        List<InventorySnapshot> inventory =
                selection != null && selection.products()
                        ? inventoryRepository.findAllItems()
                                .stream()
                                .map(this::toInventorySnapshot)
                                .toList()
                        : List.of();

        List<AttendanceSnapshot> attendance =
                selection != null && selection.attendance()
                        ? attendanceRepository.findAllSessions()
                                .stream()
                                .map(this::toAttendanceSnapshot)
                                .toList()
                        : List.of();

        List<PayrollSnapshot> payroll =
                selection != null && selection.payroll()
                        ? payrollRepository.findPayrollRecords()
                                .stream()
                                .map(this::toPayrollSnapshot)
                                .toList()
                        : List.of();

        List<SaleSnapshot> sales =
                selection != null && selection.sales()
                        ? saleRepository.findAllOrders()
                                .stream()
                                .map(this::toSaleSnapshot)
                                .toList()
                        : List.of();

        List<SaleItemSnapshot> saleItems =
                selection != null && selection.sales()
                        ? saleRepository.findAllOrderItems()
                                .stream()
                                .map(this::toSaleItemSnapshot)
                                .toList()
                        : List.of();

        List<AuditLogSnapshot> auditLogs =
                selection != null && selection.auditLogs()
                        ? auditLogRepository.findAll(null)
                                .stream()
                                .map(this::toAuditLogSnapshot)
                                .toList()
                        : List.of();

        if (selection == null || !selection.images()) {
            imageAssets.clear();
        }

        return new OfflineExportBundle(
                branches,
                users,
                employees,
                categories,
                products,
                inventory,
                attendance,
                payroll,
                sales,
                saleItems,
                auditLogs,
                imageAssets
        );
    }

    private BranchSnapshot toBranchSnapshot(
            Branch branch
    ) {

        return new BranchSnapshot(
                branch.getId(),
                branch.getName(),
                branch.getAddress(),
                branch.isActive()
        );
    }

    private UserSnapshot toUserSnapshot(
            User user
    ) {

        return new UserSnapshot(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }

    private EmployeeSnapshot toEmployeeSnapshot(
            Employee employee,
            boolean includeImages,
            List<ExportImageAsset> imageAssets
    ) {

        String imagePath =
                includeImages
                        ? resolveImagePath(
                                "employee",
                                employee.getId(),
                                employee.getImagePath(),
                                imageAssets
                        )
                        : null;

        return new EmployeeSnapshot(
                employee.getId(),
                employee.getFullName(),
                employee.getPhone(),
                employee.getAddress(),
                employee.getPosition(),
                employee.isActive(),
                employee.getUserId(),
                imagePath
        );
    }

    private CategorySnapshot toCategorySnapshot(
            Category category,
            boolean includeImages,
            List<ExportImageAsset> imageAssets
    ) {

        String imagePath =
                includeImages
                        ? resolveImagePath(
                                "category",
                                category.getId(),
                                category.getImagePath(),
                                imageAssets
                        )
                        : null;

        return new CategorySnapshot(
                category.getId(),
                category.getName(),
                category.isActive(),
                imagePath
        );
    }

    private ProductSnapshot toProductSnapshot(
            Product product,
            boolean includeImages,
            List<ExportImageAsset> imageAssets
    ) {

        String imagePath =
                includeImages
                        ? resolveImagePath(
                                "product",
                                product.getId(),
                                product.getImagePath(),
                                imageAssets
                        )
                        : null;

        return new ProductSnapshot(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getBarcode(),
                product.getCategoryId(),
                product.getBasePrice(),
                product.getUnit(),
                product.isActive(),
                imagePath
        );
    }

    private InventorySnapshot toInventorySnapshot(
            InventoryItem item
    ) {

        return new InventorySnapshot(
                item.getId(),
                item.getProductId(),
                item.getQuantity(),
                item.getUpdatedAt()
        );
    }

    private AttendanceSnapshot toAttendanceSnapshot(
            AttendanceSession session
    ) {

        return new AttendanceSnapshot(
                session.getId(),
                session.getEmployeeId(),
                session.getBranchId(),
                session.getCheckInTime(),
                session.getCheckOutTime(),
                session.getWorkedHours(),
                session.getCreatedByUserId(),
                session.getCreatedAt()
        );
    }

    private PayrollSnapshot toPayrollSnapshot(
            PayrollRecord record
    ) {

        return new PayrollSnapshot(
                record.getId(),
                record.getEmployeeId(),
                record.getMonth(),
                record.getYear(),
                record.getTotalHours(),
                record.getHourlyRateSnapshot(),
                record.getTotalSalary(),
                record.getGeneratedAt(),
                record.getGeneratedByUserId()
        );
    }

    private SaleSnapshot toSaleSnapshot(
            SaleOrder order
    ) {

        return new SaleSnapshot(
                order.getId(),
                order.getCreatedByUserId(),
                order.getTotalAmount(),
                order.getCreatedAt()
        );
    }

    private SaleItemSnapshot toSaleItemSnapshot(
            SaleOrderItem item
    ) {

        return new SaleItemSnapshot(
                item.getId(),
                item.getOrderId(),
                item.getProductId(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal()
        );
    }

    private AuditLogSnapshot toAuditLogSnapshot(
            AuditLogViewDto log
    ) {

        return new AuditLogSnapshot(
                log.getId(),
                log.getUserId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDetails(),
                log.getBranchId(),
                log.getCreatedAt()
        );
    }

    private String resolveImagePath(
            String sourceType,
            Long sourceId,
            String sourcePath,
            List<ExportImageAsset> imageAssets
    ) {

        File sourceFile =
                imageStorageService.resolveImageFile(sourcePath);

        if (sourceFile == null || !sourceFile.isFile()) {
            if (sourcePath != null && !sourcePath.trim().isEmpty()) {
                File fallbackFile =
                        new File(sourcePath.trim());

                if (fallbackFile.isFile()) {
                    sourceFile = fallbackFile;
                }
            }
        }

        if (sourceFile == null || !sourceFile.isFile()) {
            return null;
        }

        String exportPath =
                buildExportImagePath(
                        sourceType,
                        sourceId,
                        sourceFile
                );

        imageAssets.add(
                new ExportImageAsset(
                        sourceType,
                        sourceId,
                        sourcePath,
                        exportPath,
                        sourceFile
                )
        );

        return exportPath;
    }

    private String buildExportImagePath(
            String sourceType,
            Long sourceId,
            File sourceFile
    ) {

        String extension =
                safeExtension(sourceFile.getName());

        return OfflineExportSpecV1.IMAGES_DIRECTORY
                + "/"
                + sourceType
                + "-"
                + sourceId
                + extension;
    }

    private String safeExtension(
            String fileName
    ) {

        if (fileName == null) {
            return ".png";
        }

        int dotIndex =
                fileName.lastIndexOf('.');

        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return ".png";
        }

        String extension =
                fileName.substring(dotIndex).toLowerCase();

        return switch (extension) {
            case ".png", ".jpg", ".jpeg", ".gif", ".bmp", ".webp" ->
                    extension;
            default -> ".png";
        };
    }
}
