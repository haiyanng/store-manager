package com.storemanager.domain.sale.service;

import com.storemanager.core.security.PermissionGuard;
import com.storemanager.core.session.AppSession;
import com.storemanager.domain.audit.service.AuditService;
import com.storemanager.domain.inventory.model.InventoryItem;
import com.storemanager.domain.inventory.model.InventoryTransaction;
import com.storemanager.domain.inventory.model.InventoryTransactionType;
import com.storemanager.domain.inventory.service.InventoryService;
import com.storemanager.domain.notification.model.NotificationType;
import com.storemanager.domain.notification.service.NotificationService;
import com.storemanager.domain.product.model.Product;
import com.storemanager.domain.product.service.ProductService;
import com.storemanager.domain.sale.model.SaleCartItem;
import com.storemanager.domain.sale.model.SaleOrder;
import com.storemanager.domain.sale.model.SaleOrderItem;
import com.storemanager.domain.sale.model.SelectedProductPreviewDto;
import com.storemanager.domain.sale.repository.SaleRepository;
import com.storemanager.domain.user.model.User;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    private final AuditService auditService =
            new AuditService();

    private final NotificationService notificationService =
            new NotificationService();

    public List<Product> findProducts() {

        validateSaleAccess();

        return productService.findAll();
    }

    public List<Product> findQuickPickProducts(
            int limit
    ) {

        validateSaleAccess();

        return resolveProductsByIds(
                saleRepository.findTopSellingProductIds(limit)
        );
    }

    public List<SaleOrder> findRecentOrders() {

        validateSaleAccess();

        return saleRepository.findRecentOrders();
    }

    public Map<Long, Integer> findCurrentStockByProductId() {

        validateSaleAccess();

        if (!PermissionGuard.canViewInventory()) {
            return Map.of();
        }

        return inventoryService.findAllItems()
                .stream()
                .collect(
                        Collectors.toMap(
                                InventoryItem::getProductId,
                                InventoryItem::getQuantity
                        )
                );
    }

    public SelectedProductPreviewDto buildSelectedProductPreview(
            Product product,
            Map<Long, Integer> stockByProductId
    ) {

        if (product == null) {
            return null;
        }

        Integer stockQuantity =
                null;

        if (stockByProductId != null
                && product.getId() != null) {
            stockQuantity =
                    stockByProductId.get(product.getId());
        }

        return new SelectedProductPreviewDto(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getBarcode(),
                product.getImagePath(),
                product.getBasePrice() == null
                        ? BigDecimal.ZERO
                        : product.getBasePrice(),
                stockQuantity
        );
    }

    public BigDecimal findTotalRevenue() {

        validateSaleAccess();

        return saleRepository.findTotalRevenue();
    }

    public BigDecimal findRevenueForCurrentMonth() {

        validateSaleAccess();

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.withDayOfMonth(1);
        LocalDate endDate = startDate.plusMonths(1);

        return saleRepository.findRevenueForPeriod(startDate, endDate);
    }

    public long countOrdersForCurrentMonth() {

        validateSaleAccess();

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.withDayOfMonth(1);
        LocalDate endDate = startDate.plusMonths(1);

        return saleRepository.countOrdersForPeriod(startDate, endDate);
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

        auditService.record(
                AuditService.ACTION_SALE_FINALIZED,
                "SALE_ORDER",
                orderId,
                "Sale finalized with " + orderItems.size() + " items",
                null
        );

        notificationService.notifyCurrentUser(
                "Sale finalized",
                "Sale order #"
                        + orderId
                        + " was saved",
                NotificationType.INVENTORY
        );

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

    private void validateSaleAccess() {

        if (!PermissionGuard.canViewOrder()) {
            auditService.recordPermissionDenied(
                    AuditService.ACTION_PERMISSION_DENIED,
                    "SALE",
                    null,
                    "Sale access denied",
                    null
            );
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
