package com.storemanager.domain.sale.service;

import com.storemanager.core.security.PermissionGuard;
import com.storemanager.core.session.AppSession;
import com.storemanager.domain.inventory.model.InventoryItem;
import com.storemanager.domain.inventory.model.InventoryTransaction;
import com.storemanager.domain.inventory.model.InventoryTransactionType;
import com.storemanager.domain.inventory.service.InventoryService;
import com.storemanager.domain.product.model.Product;
import com.storemanager.domain.product.service.ProductService;
import com.storemanager.domain.sale.model.SaleCartItem;
import com.storemanager.domain.sale.model.SaleOrder;
import com.storemanager.domain.sale.model.SaleOrderItem;
import com.storemanager.domain.sale.repository.SaleRepository;
import com.storemanager.domain.user.model.User;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SaleService {

    private final SaleRepository saleRepository =
            new SaleRepository();

    private final ProductService productService =
            new ProductService();

    private final InventoryService inventoryService =
            new InventoryService();

    public List<Product> findProducts() {

        validateSaleAccess();

        return productService.findAll();
    }

    public List<SaleOrder> findRecentOrders() {

        validateSaleAccess();

        return saleRepository.findRecentOrders();
    }

    public Long finalizeSale(
            List<SaleCartItem> cartItems
    ) {

        validateSaleAccess();
        validateCart(cartItems);
        validateStockAvailability(cartItems);

        SaleOrder order =
                buildOrder(cartItems);

        List<SaleOrderItem> orderItems =
                buildOrderItems(cartItems);

        Long orderId =
                saleRepository.saveOrder(
                        order,
                        orderItems
                );

        if (orderId == null) {
            throw new RuntimeException(
                    "Cannot create sale order"
            );
        }

        for (SaleOrderItem item : orderItems) {
            createInventorySaleTransaction(
                    orderId,
                    item
            );
        }

        return orderId;
    }

    public BigDecimal calculateTotal(
            List<SaleCartItem> cartItems
    ) {

        if (cartItems == null || cartItems.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return cartItems
                .stream()
                .map(SaleCartItem::getSubtotal)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }

    private SaleOrder buildOrder(
            List<SaleCartItem> cartItems
    ) {

        SaleOrder order =
                new SaleOrder();

        User currentUser =
                AppSession.getCurrentUser();

        order.setCreatedByUserId(
                currentUser == null
                        ? null
                        : currentUser.getId()
        );

        order.setTotalAmount(
                calculateTotal(cartItems)
        );

        return order;
    }

    private List<SaleOrderItem> buildOrderItems(
            List<SaleCartItem> cartItems
    ) {

        List<SaleOrderItem> orderItems =
                new ArrayList<>();

        for (SaleCartItem cartItem : cartItems) {
            SaleOrderItem item =
                    new SaleOrderItem();

            item.setProductId(
                    cartItem.getProduct().getId()
            );
            item.setQuantity(
                    cartItem.getQuantity()
            );
            item.setUnitPrice(
                    cartItem.getUnitPrice()
            );
            item.setSubtotal(
                    cartItem.getSubtotal()
            );

            orderItems.add(item);
        }

        return orderItems;
    }

    private void createInventorySaleTransaction(
            Long orderId,
            SaleOrderItem item
    ) {

        InventoryTransaction transaction =
                new InventoryTransaction();

        transaction.setProductId(
                item.getProductId()
        );
        transaction.setType(
                InventoryTransactionType.SALE
        );
        transaction.setQuantity(
                item.getQuantity()
        );
        transaction.setReason(
                "Sale order #" + orderId
        );

        boolean success =
                inventoryService.adjustStock(transaction);

        if (!success) {
            throw new RuntimeException(
                    "Cannot reduce inventory for product "
                            + item.getProductId()
            );
        }
    }

    private void validateSaleAccess() {

        if (!PermissionGuard.canViewOrder()) {
            throw new RuntimeException(
                    "Sale access denied"
            );
        }
    }

    private void validateCart(
            List<SaleCartItem> cartItems
    ) {

        if (cartItems == null || cartItems.isEmpty()) {
            throw new RuntimeException(
                    "Cart is empty"
            );
        }

        for (SaleCartItem cartItem : cartItems) {
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
        }
    }

    private void validateStockAvailability(
            List<SaleCartItem> cartItems
    ) {

        Map<Long, Integer> quantityByProductId =
                inventoryService
                        .findAllItems()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        InventoryItem::getProductId,
                                        InventoryItem::getQuantity
                                )
                        );

        for (SaleCartItem cartItem : cartItems) {
            Long productId =
                    cartItem.getProduct().getId();

            int currentQuantity =
                    quantityByProductId.getOrDefault(
                            productId,
                            0
                    );

            if (currentQuantity < cartItem.getQuantity()) {
                throw new RuntimeException(
                        "Insufficient stock for "
                                + cartItem.getProduct().getName()
                );
            }
        }
    }
}
