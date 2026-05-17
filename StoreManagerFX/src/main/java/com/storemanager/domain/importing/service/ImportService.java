package com.storemanager.domain.importing.service;

import com.storemanager.core.security.PermissionGuard;
import com.storemanager.core.session.AppSession;
import com.storemanager.domain.audit.service.AuditService;
import com.storemanager.domain.importing.model.ImportCartItem;
import com.storemanager.domain.importing.model.ImportItem;
import com.storemanager.domain.importing.model.ImportReceipt;
import com.storemanager.domain.importing.repository.ImportRepository;
import com.storemanager.domain.inventory.model.InventoryTransaction;
import com.storemanager.domain.inventory.model.InventoryTransactionType;
import com.storemanager.domain.inventory.service.InventoryService;
import com.storemanager.domain.notification.model.NotificationType;
import com.storemanager.domain.notification.service.NotificationService;
import com.storemanager.domain.product.model.Product;
import com.storemanager.domain.product.service.ProductService;
import com.storemanager.domain.user.model.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ImportService {

    private final ImportRepository importRepository =
            new ImportRepository();

    private final ProductService productService =
            new ProductService();

    private final InventoryService inventoryService =
            new InventoryService();

    private final AuditService auditService =
            new AuditService();

    private final NotificationService notificationService =
            new NotificationService();

    public List<Product> findProducts() {

        validateImportAccess();

        return productService.findAll();
    }

    public List<ImportReceipt> findRecentReceipts() {

        validateImportAccess();

        return importRepository.findRecentReceipts();
    }

    public BigDecimal findTotalImportCost() {

        validateImportAccess();

        return importRepository.findTotalImportCost();
    }

    public BigDecimal findImportCostForCurrentMonth() {

        validateImportAccess();

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.withDayOfMonth(1);
        LocalDate endDate = startDate.plusMonths(1);

        return importRepository.findImportCostForPeriod(startDate, endDate);
    }

    public long countReceiptsForCurrentMonth() {

        validateImportAccess();

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.withDayOfMonth(1);
        LocalDate endDate = startDate.plusMonths(1);

        return importRepository.countReceiptsForPeriod(startDate, endDate);
    }

    public Long finalizeImport(
            String supplierName,
            List<ImportCartItem> cartItems
    ) {

        validateImportAccess();
        validateImport(supplierName, cartItems);

        ImportReceipt receipt =
                buildReceipt(
                        supplierName,
                        cartItems
                );

        List<ImportItem> importItems =
                buildImportItems(cartItems);

        Long receiptId =
                importRepository.saveReceipt(
                        receipt,
                        importItems
                );

        if (receiptId == null) {
            throw new RuntimeException(
                    "Cannot create import receipt"
            );
        }

        for (ImportItem item : importItems) {
            createInventoryImportTransaction(
                    receiptId,
                    item
            );
        }

        auditService.record(
                AuditService.ACTION_IMPORT_FINALIZED,
                "IMPORT_RECEIPT",
                receiptId,
                "Import finalized for supplier " + receipt.getSupplierName(),
                null
        );

        notificationService.notifyCurrentUser(
                "Import finalized",
                "Import receipt #"
                        + receiptId
                        + " was saved",
                NotificationType.INVENTORY
        );

        return receiptId;
    }

    public BigDecimal calculateTotal(
            List<ImportCartItem> cartItems
    ) {

        if (cartItems == null || cartItems.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return cartItems
                .stream()
                .map(ImportCartItem::getSubtotal)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }

    private ImportReceipt buildReceipt(
            String supplierName,
            List<ImportCartItem> cartItems
    ) {

        ImportReceipt receipt =
                new ImportReceipt();

        User currentUser =
                AppSession.getCurrentUser();

        receipt.setSupplierName(
                supplierName.trim()
        );
        receipt.setTotalCost(
                calculateTotal(cartItems)
        );
        receipt.setCreatedByUserId(
                currentUser == null
                        ? null
                        : currentUser.getId()
        );

        return receipt;
    }

    private List<ImportItem> buildImportItems(
            List<ImportCartItem> cartItems
    ) {

        List<ImportItem> importItems =
                new ArrayList<>();

        for (ImportCartItem cartItem : cartItems) {
            ImportItem item =
                    new ImportItem();

            item.setProductId(
                    cartItem.getProduct().getId()
            );
            item.setQuantity(
                    cartItem.getQuantity()
            );
            item.setUnitCost(
                    cartItem.getUnitCost()
            );
            item.setSubtotal(
                    cartItem.getSubtotal()
            );

            importItems.add(item);
        }

        return importItems;
    }

    private void createInventoryImportTransaction(
            Long receiptId,
            ImportItem item
    ) {

        InventoryTransaction transaction =
                new InventoryTransaction();

        transaction.setProductId(
                item.getProductId()
        );
        transaction.setType(
                InventoryTransactionType.IMPORT
        );
        transaction.setQuantity(
                item.getQuantity()
        );
        transaction.setReason(
                "Import receipt #" + receiptId
        );

        boolean success =
                inventoryService.adjustStock(transaction);

        if (!success) {
            throw new RuntimeException(
                    "Cannot increase inventory for product "
                            + item.getProductId()
            );
        }
    }

    private void validateImportAccess() {

        if (!PermissionGuard.canViewInventory()) {
            auditService.recordPermissionDenied(
                    AuditService.ACTION_PERMISSION_DENIED,
                    "IMPORT",
                    null,
                    "Import access denied",
                    null
            );
            throw new RuntimeException(
                    "Import access denied"
            );
        }
    }

    private void validateImport(
            String supplierName,
            List<ImportCartItem> cartItems
    ) {

        if (supplierName == null || supplierName.trim().isEmpty()) {
            throw new RuntimeException(
                    "Supplier name is required"
            );
        }

        if (cartItems == null || cartItems.isEmpty()) {
            throw new RuntimeException(
                    "Import item list is empty"
            );
        }

        for (ImportCartItem cartItem : cartItems) {
            if (cartItem.getProduct() == null
                    || cartItem.getProduct().getId() == null) {
                throw new RuntimeException(
                        "Product is required"
                );
            }

            if (cartItem.getQuantity() <= 0) {
                throw new RuntimeException(
                        "Quantity must be positive"
                );
            }

            if (cartItem.getUnitCost().compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException(
                        "Unit cost cannot be negative"
                );
            }
        }
    }
}
