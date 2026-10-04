package com.storemanager.domain.inventory.service;

import com.storemanager.core.security.PermissionGuard;
import com.storemanager.core.session.AppSession;
import com.storemanager.domain.audit.service.AuditService;
import com.storemanager.domain.inventory.model.InventoryItem;
import com.storemanager.domain.inventory.model.InventoryTransaction;
import com.storemanager.domain.inventory.model.InventoryTransactionType;
import com.storemanager.domain.inventory.repository.InventoryRepository;
import com.storemanager.domain.product.model.Product;
import com.storemanager.domain.product.service.ProductService;
import com.storemanager.domain.notification.model.NotificationType;
import com.storemanager.domain.notification.service.NotificationService;
import com.storemanager.domain.user.model.User;

import java.util.List;
import java.time.LocalDate;
import java.util.Map;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.stream.Collectors;

public class InventoryService {

    private final InventoryRepository inventoryRepository =
            new InventoryRepository();

    private final ProductService productService =
            new ProductService();

    private final AuditService auditService =
            new AuditService();

    private final NotificationService notificationService =
            new NotificationService();

    public List<InventoryItem> findAllItems() {

        validateInventoryAccess();

        return inventoryRepository.findAllItems();
    }

    public List<InventoryItem> findItemsWithExpiry() {
        List<InventoryItem> items = findAllItems();
        Map<Long, LocalDate> dates = inventoryRepository.findNearestExpiryDates();
        LocalDate today = LocalDate.now();
        for (InventoryItem item : items) {
            LocalDate expiry = dates.get(item.getProductId());
            item.setNearestExpiryDate(expiry);
            item.setExpiryStatus(expiryStatus(expiry, today));
        }
        return items;
    }

    public static String expiryStatus(LocalDate expiry, LocalDate today) {
        if (expiry == null) return "N/A";
        if (expiry.isBefore(today)) return "EXPIRED";
        if (!expiry.isAfter(today.plusDays(7))) return "EXPIRING SOON";
        return "NORMAL";
    }

    public static boolean isLowStock(InventoryItem item) { return item.getQuantity() < 10; }

    public static List<InventoryTransaction> filterImportHistory(List<InventoryTransaction> rows,
            LocalDate from, LocalDate to, Long productId) {
        if (from != null && to != null && from.isAfter(to))
            throw new IllegalArgumentException("Start date must be on or before end date.");
        return rows.stream().filter(row -> row.getType() == InventoryTransactionType.IMPORT)
                .filter(row -> productId == null || productId.equals(row.getProductId()))
                .filter(row -> from == null || row.getCreatedAt() != null
                        && !row.getCreatedAt().toLocalDate().isBefore(from))
                .filter(row -> to == null || row.getCreatedAt() != null
                        && !row.getCreatedAt().toLocalDate().isAfter(to)).toList();
    }

    public List<InventoryTransaction> findAllTransactions() {

        validateInventoryAccess();

        return inventoryRepository.findAllTransactions();
    }

    public List<Product> findProducts() {

        validateInventoryAccess();

        return productService.findAll();
    }

    public List<Product> findQuickPickProducts(
            int limit
    ) {

        validateInventoryAccess();

        return resolveProductsByIds(
                inventoryRepository.findRecentProductIds(limit)
        );
    }

    public Map<Long, Product> findProductsById() {

        validateInventoryAccess();

        return productService
                .findAllIncludingInactive()
                .stream()
                .collect(
                        Collectors.toMap(
                                Product::getId,
                                product -> product
                        )
                );
    }

    public BigDecimal findInventoryValue() {

        validateInventoryAccess();

        Map<Long, Product> productsById =
                findProductsById();

        return inventoryRepository.findAllItems()
                .stream()
                .map(item -> {
                    Product product = productsById.get(item.getProductId());
                    if (product == null || product.getBasePrice() == null) {
                        return BigDecimal.ZERO;
                    }

                    return product.getBasePrice().multiply(
                            BigDecimal.valueOf(item.getQuantity())
                    );
                })
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }

    public long findInventoryItemCount() {

        validateInventoryAccess();

        return inventoryRepository.findAllItems().size();
    }

    public boolean adjustStock(
            InventoryTransaction transaction
    ) {

        validateInventoryAdjustmentAccess("INVENTORY_ADJUSTMENT");
        validateTransaction(transaction);

        User currentUser =
                AppSession.getCurrentUser();

        transaction.setCreatedByUserId(
                currentUser == null
                        ? null
                        : currentUser.getId()
        );

        transaction.setReason(
                clean(transaction.getReason())
        );

        boolean applied =
                inventoryRepository.applyTransaction(
                transaction,
                calculateQuantityDelta(transaction)
        );

        if (applied) {
            auditStockAdjustment(transaction);
            notifyInventoryChange(transaction);
        }

        return applied;
    }

    public boolean adjustStock(
            Connection connection,
            InventoryTransaction transaction
    ) {

        validateInventoryWorkflowAccess(transaction);
        validateTransaction(transaction);

        User currentUser =
                AppSession.getCurrentUser();

        transaction.setCreatedByUserId(
                currentUser == null
                        ? null
                        : currentUser.getId()
        );

        transaction.setReason(
                clean(transaction.getReason())
        );

        return inventoryRepository.applyTransaction(
                connection,
                transaction,
                calculateQuantityDelta(transaction)
        );
    }

    private void validateInventoryAccess() {

        if (!PermissionGuard.canViewInventory()) {
            auditService.recordPermissionDenied("INVENTORY_VIEW", "INVENTORY", null, "Inventory access denied", "OWNER/MANAGER/STAFF");
            throw new RuntimeException(
                    "Inventory access denied"
            );
        }
    }

    private void validateInventoryAdjustmentAccess(
            String action
    ) {

        if (!PermissionGuard.canAdjustInventory()) {
            auditService.recordPermissionDenied(action, "INVENTORY", null, "Inventory adjustment denied", "OWNER/MANAGER");
            throw new RuntimeException(
                    "Current user cannot adjust inventory"
            );
        }
    }

    private void validateInventoryWorkflowAccess(
            InventoryTransaction transaction
    ) {

        InventoryTransactionType type =
                transaction == null ? null : transaction.getType();

        boolean allowed;

        if (type == InventoryTransactionType.SALE) {
            allowed =
                    PermissionGuard.canCreateSale();
        } else {
            allowed =
                    PermissionGuard.canAdjustInventory();
        }

        if (!allowed) {
            auditService.recordPermissionDenied("INVENTORY_TRANSACTION", "INVENTORY", null, "Inventory transaction denied", "OWNER/MANAGER");
            throw new RuntimeException(
                    "Current user cannot update inventory"
            );
        }
    }

    private void validateTransaction(
            InventoryTransaction transaction
    ) {

        if (transaction == null) {
            throw new RuntimeException(
                    "Inventory transaction is required"
            );
        }

        if (transaction.getProductId() == null) {
            throw new RuntimeException(
                    "Product is required"
            );
        }

        if (transaction.getType() == null) {
            throw new RuntimeException(
                    "Transaction type is required"
            );
        }

        if (transaction.getQuantity() == 0) {
            throw new RuntimeException(
                    "Quantity cannot be zero"
            );
        }

        if (transaction.getType() == InventoryTransactionType.ADJUSTMENT
                && transaction.getQuantity() < 0
                && (transaction.getReason() == null
                || transaction.getReason().trim().isEmpty())) {
            throw new RuntimeException(
                    "Reason is required for stock decrease"
            );
        }

        if (transaction.getType() != InventoryTransactionType.ADJUSTMENT
                && transaction.getQuantity() < 0) {
            throw new RuntimeException(
                    "Quantity must be positive"
            );
        }
    }

    private void auditStockAdjustment(
            InventoryTransaction transaction
    ) {

        if (transaction == null) {
            return;
        }

        auditService.recordEvent(
                "INVENTORY",
                transaction.getType() == InventoryTransactionType.IMPORT
                        ? "IMPORT_CREATE"
                        : "INVENTORY_ADJUSTMENT",
                "INVENTORY_TRANSACTION",
                transaction.getProductId(),
                true,
                transaction.getReason(),
                "{\"type\":\"" + transaction.getType()
                        + "\",\"quantity\":"
                        + transaction.getQuantity()
                        + "}"
        );
    }

    private int calculateQuantityDelta(
            InventoryTransaction transaction
    ) {

        return switch (transaction.getType()) {
            case IMPORT -> transaction.getQuantity();
            case SALE, DAMAGE -> -transaction.getQuantity();
            case ADJUSTMENT -> transaction.getQuantity();
        };
    }

    private String clean(
            String value
    ) {

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }

    private void notifyInventoryChange(
            InventoryTransaction transaction
    ) {

        User currentUser =
                AppSession.getCurrentUser();

        if (currentUser == null) {
            return;
        }

        Product product =
                productService.findAll()
                        .stream()
                        .filter(item ->
                                item.getId() != null
                                        && item.getId().equals(transaction.getProductId())
                        )
                        .findFirst()
                        .orElse(null);

        String productName =
                product == null ? "Product #" + transaction.getProductId() : product.getName();

        notificationService.notifyUser(
                currentUser.getId(),
                "Inventory updated",
                transaction.getType()
                        + " "
                        + transaction.getQuantity()
                        + " for "
                        + productName,
                NotificationType.INVENTORY
        );

        int remainingQuantity =
                inventoryRepository.findAllItems()
                        .stream()
                        .filter(item ->
                                item.getProductId() != null
                                        && item.getProductId().equals(transaction.getProductId())
                        )
                        .map(InventoryItem::getQuantity)
                        .findFirst()
                        .orElse(0);

        if (remainingQuantity <= 5) {
            notificationService.notifyUser(
                    currentUser.getId(),
                    "Low stock alert",
                    productName + " has low stock: " + remainingQuantity,
                    NotificationType.INVENTORY
            );
        }
    }

    private List<Product> resolveProductsByIds(
            List<Long> productIds
    ) {

        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Product> productsById =
                productService.findAll()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        Product::getId,
                                        product -> product
                                )
                        );

        List<Product> products =
                new java.util.ArrayList<>();

        for (Long productId : productIds) {
            Product product =
                    productsById.get(productId);
            if (product != null) {
                products.add(product);
            }
        }

        return products;
    }
}
