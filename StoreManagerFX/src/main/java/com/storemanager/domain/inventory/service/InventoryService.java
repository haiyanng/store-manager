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
import java.util.Map;
import java.math.BigDecimal;
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

    public List<InventoryTransaction> findAllTransactions() {

        validateInventoryAccess();

        return inventoryRepository.findAllTransactions();
    }

    public List<Product> findProducts() {

        validateInventoryAccess();

        return productService.findAll();
    }

    public Map<Long, Product> findProductsById() {

        validateInventoryAccess();

        return productService
                .findAll()
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

        validateInventoryAccess();
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

    private void validateInventoryAccess() {

        if (!PermissionGuard.canViewInventory()) {
            auditService.recordPermissionDenied(
                    AuditService.ACTION_PERMISSION_DENIED,
                    "INVENTORY",
                    null,
                    "Inventory access denied",
                    null
            );
            throw new RuntimeException(
                    "Inventory access denied"
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

        auditService.record(
                AuditService.ACTION_INVENTORY_ADJUSTMENT,
                "INVENTORY_TRANSACTION",
                transaction.getProductId(),
                transaction.getType()
                        + " "
                        + transaction.getQuantity()
                        + " | "
                        + transaction.getReason(),
                null
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
}
