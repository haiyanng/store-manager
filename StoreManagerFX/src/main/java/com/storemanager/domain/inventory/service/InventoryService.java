package com.storemanager.domain.inventory.service;

import com.storemanager.core.security.PermissionGuard;
import com.storemanager.core.session.AppSession;
import com.storemanager.domain.inventory.model.InventoryItem;
import com.storemanager.domain.inventory.model.InventoryTransaction;
import com.storemanager.domain.inventory.model.InventoryTransactionType;
import com.storemanager.domain.inventory.repository.InventoryRepository;
import com.storemanager.domain.product.model.Product;
import com.storemanager.domain.product.service.ProductService;
import com.storemanager.domain.user.model.User;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InventoryService {

    private final InventoryRepository inventoryRepository =
            new InventoryRepository();

    private final ProductService productService =
            new ProductService();

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

        return inventoryRepository.applyTransaction(
                transaction,
                calculateQuantityDelta(transaction)
        );
    }

    private void validateInventoryAccess() {

        if (!PermissionGuard.canViewInventory()) {
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
}
