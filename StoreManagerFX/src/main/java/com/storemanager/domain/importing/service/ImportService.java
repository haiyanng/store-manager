package com.storemanager.domain.importing.service;

import com.storemanager.core.security.PermissionGuard;
import com.storemanager.core.session.AppSession;
import com.storemanager.core.database.ConnectionFactory;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

    public List<Product> findQuickPickProducts(
            int limit
    ) {

        validateImportAccess();

        return resolveProductsByIds(
                importRepository.findRecentProductIds(limit)
        );
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

        try {
            validateImportAccess();
            validateImport(supplierName, cartItems);
        } catch (RuntimeException e) {
            auditService.recordEvent(
                    "IMPORT",
                    "IMPORT_CREATE",
                    "IMPORT_RECEIPT",
                    null,
                    false,
                    rootMessage(e),
                    "{\"supplier\":\"" + escape(supplierName) + "\"}"
            );
            throw e;
        }

        ImportReceipt receipt =
                buildReceipt(
                        supplierName,
                        cartItems
                );

        List<ImportItem> importItems =
                buildImportItems(cartItems);

        Long receiptId;

        try {
            receiptId =
                    persistImportAndInventoryAtomically(
                            receipt,
                            importItems
                    );
        } catch (RuntimeException e) {
            auditService.recordEvent(
                    "IMPORT",
                    "IMPORT_CREATE",
                    "IMPORT_RECEIPT",
                    null,
                    false,
                    rootMessage(e),
                    "{\"items\":" + importItems.size() + "}"
            );
            throw e;
        }

        auditService.recordEvent(
                "IMPORT",
                "IMPORT_CREATE",
                "IMPORT_RECEIPT",
                receiptId,
                true,
                null,
                "{\"supplier\":\"" + escape(receipt.getSupplierName()) + "\",\"items\":" + importItems.size() + "}"
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

    private Long persistImportAndInventoryAtomically(
            ImportReceipt receipt,
            List<ImportItem> importItems
    ) {

        try (
                Connection connection =
                        ConnectionFactory.getConnection()
        ) {
            connection.setAutoCommit(false);

            try {
                validateProductsActive(
                        connection,
                        importItems
                );

                Long receiptId =
                        importRepository.saveReceipt(
                                connection,
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
                            connection,
                            receiptId,
                            item
                    );
                }

                connection.commit();
                return receiptId;
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    "Cannot finalize import atomically",
                    e
            );
        }
    }

    private void validateProductsActive(
            Connection connection,
            List<ImportItem> importItems
    ) throws Exception {

        try (
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                SELECT active
                                FROM products
                                WHERE id = ?
                                FOR UPDATE
                                """
                        )
        ) {
            for (ImportItem item : importItems) {
                statement.setLong(1, item.getProductId());

                try (
                        ResultSet resultSet =
                                statement.executeQuery()
                ) {
                    if (!resultSet.next()
                            || !resultSet.getBoolean("active")) {
                        throw new RuntimeException(
                                "Product is inactive and cannot be sold/imported."
                        );
                    }
                }
            }
        }
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
            Connection connection,
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
                inventoryService.adjustStock(
                        connection,
                        transaction
                );

        if (!success) {
            throw new RuntimeException(
                    "Cannot increase inventory for product "
                            + item.getProductId()
            );
        }
    }

    private void validateImportAccess() {

        if (!PermissionGuard.canAdjustInventory()) {
            auditService.recordPermissionDenied(
                    "IMPORT_CREATE",
                    "IMPORT",
                    null,
                    "Import access denied",
                    null,
                    "OWNER/MANAGER"
            );
            throw new RuntimeException(
                    "Current user cannot import inventory"
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

            if (!cartItem.getProduct().isActive()) {
                throw new RuntimeException(
                        "Product is inactive and cannot be sold/imported."
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

            if (cartItem.getUnitCost().stripTrailingZeros().scale() > 2) {
                throw new IllegalArgumentException("Unit cost must have at most 2 decimal places");
            }
        }
    }

    private List<Product> resolveProductsByIds(
            List<Long> productIds
    ) {

        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }

        java.util.Map<Long, Product> productsById =
                productService.findAll()
                        .stream()
                        .collect(
                                java.util.stream.Collectors.toMap(
                                        Product::getId,
                                        product -> product
                                )
                        );

        List<Product> products =
                new ArrayList<>();

        for (Long productId : productIds) {
            Product product =
                    productsById.get(productId);
            if (product != null) {
                products.add(product);
            }
        }

        return products;
    }

    private String rootMessage(
            Throwable throwable
    ) {

        Throwable current =
                throwable;

        while (current.getCause() != null) {
            current =
                    current.getCause();
        }

        return current.getMessage();
    }

    private String escape(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
