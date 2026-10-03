package com.storemanager.domain.sale.service;

import com.storemanager.core.security.PermissionGuard;
import com.storemanager.core.session.AppSession;
import com.storemanager.core.database.ConnectionFactory;
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
import com.storemanager.domain.sale.model.SaleOrderItemDetail;
import com.storemanager.domain.sale.model.SelectedProductPreviewDto;
import com.storemanager.domain.sale.repository.SaleRepository;
import com.storemanager.domain.user.model.User;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

    public List<SaleOrderItemDetail> findOrderItemDetails(
            SaleOrder order
    ) {

        validateSaleAccess();

        if (order == null || order.getId() == null) {
            return List.of();
        }

        Map<Long, Product> productsById =
                productService.findAllIncludingInactive()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        Product::getId,
                                        product -> product
                                )
                        );

        return saleRepository.findOrderItemsByOrderId(order.getId())
                .stream()
                .map(item ->
                        toOrderItemDetail(
                                item,
                                productsById.get(item.getProductId())
                        )
                )
                .toList();
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

    public SaleOrder finalizeSale(
            List<SaleCartItem> cartItems,
            BigDecimal amountReceived
    ) {

        try {
            validateSaleCreateAccess();
            validateCart(cartItems);
            validatePayment(calculateTotal(cartItems), amountReceived);
            validateStockAvailability(cartItems);
        } catch (RuntimeException e) {
            auditService.recordEvent(
                    "SALE",
                    "SALE_CREATE",
                    "SALE_ORDER",
                    null,
                    false,
                    rootMessage(e),
                    "{}"
            );
            throw e;
        }

        SaleOrder order =
                buildOrder(cartItems);
        order.setAmountReceived(amountReceived);
        order.setChangeAmount(amountReceived.subtract(order.getTotalAmount()));

        List<SaleOrderItem> orderItems =
                buildOrderItems(cartItems);

        Long orderId;

        try {
            orderId =
                    persistSaleAndInventoryAtomically(
                            order,
                            orderItems
                    );
        } catch (RuntimeException e) {
            auditService.recordEvent(
                    "SALE",
                    "SALE_CREATE",
                    "SALE_ORDER",
                    null,
                    false,
                    rootMessage(e),
                    "{\"items\":" + orderItems.size() + "}"
            );
            throw e;
        }

        auditService.recordEvent(
                "SALE",
                "SALE_CREATE",
                "SALE_ORDER",
                orderId,
                true,
                null,
                "{\"items\":" + orderItems.size() + ",\"total\":\"" + order.getTotalAmount() + "\"}"
        );

        notificationService.notifyCurrentUser(
                "Order finalized",
                "Order #"
                        + orderId
                        + " was saved",
                NotificationType.INVENTORY
        );

        order.setId(orderId);
        return order;
    }

    public static BigDecimal parseAmountReceived(String text) {
        String value = text == null ? "" : text.trim();
        if (!value.matches("(?:[0-9]{1,16}|[0-9]{1,3}(?:,[0-9]{3}){1,5})(?:\\.[0-9]{1,2})?")) {
            throw new IllegalArgumentException("Enter a valid amount received, for example 1000.00 or 1,000.00");
        }
        BigDecimal amount = new BigDecimal(value.replace(",", ""));
        validateMoney(amount, "Amount received");
        return amount;
    }

    public static void validatePayment(BigDecimal total, BigDecimal amountReceived) {
        validateMoney(total, "Order total");
        validateMoney(amountReceived, "Amount received");
        if (amountReceived.compareTo(total) < 0) {
            throw new IllegalArgumentException("Amount received must be at least the order total");
        }
    }

    private static void validateMoney(BigDecimal amount, String field) {
        if (amount == null || amount.signum() < 0
                || amount.compareTo(new BigDecimal("9999999999999999.99")) > 0
                || amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(field + " must be a non-negative amount with at most 2 decimal places, up to 9,999,999,999,999,999.99");
        }
    }

    private Long persistSaleAndInventoryAtomically(
            SaleOrder order,
            List<SaleOrderItem> orderItems
    ) {

        try (
                Connection connection =
                        ConnectionFactory.getConnection()
        ) {
            connection.setAutoCommit(false);

            try {
                validateProductsActive(
                        connection,
                        orderItems
                );

                Long orderId =
                        saleRepository.saveOrder(
                                connection,
                                order,
                                orderItems
                        );

                if (orderId == null) {
                    throw new RuntimeException(
                            "Cannot create order"
                    );
                }

                for (SaleOrderItem item : orderItems) {
                    createInventorySaleTransaction(
                            connection,
                            orderId,
                            item
                    );
                }

                connection.commit();
                return orderId;
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    "Cannot finalize order atomically",
                    e
            );
        }
    }

    private void validateProductsActive(
            Connection connection,
            List<SaleOrderItem> orderItems
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
            for (SaleOrderItem item : orderItems) {
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
            Connection connection,
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
                "Order #" + orderId
        );

        boolean success =
                inventoryService.adjustStock(
                        connection,
                        transaction
                );

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

    private SaleOrderItemDetail toOrderItemDetail(
            SaleOrderItem item,
            Product product
    ) {

        SaleOrderItemDetail detail =
                new SaleOrderItemDetail();

        detail.setOrderItemId(item.getId());
        detail.setProductId(item.getProductId());
        detail.setProductName(
                product == null
                        ? "Product #" + item.getProductId()
                        : product.getName()
        );
        detail.setSku(
                product == null
                        ? "-"
                        : product.getSku()
        );
        detail.setQuantity(item.getQuantity());
        detail.setUnitPrice(item.getUnitPrice());
        detail.setSubtotal(item.getSubtotal());

        return detail;
    }

    private void validateSaleAccess() {

        if (!PermissionGuard.canViewOrder()) {
            auditService.recordPermissionDenied("SALE_VIEW", "SALE", null, "Order access denied", "OWNER/MANAGER/STAFF");
            throw new RuntimeException(
                    "Order access denied"
            );
        }
    }

    private void validateSaleCreateAccess() {

        if (!PermissionGuard.canCreateSale()) {
            auditService.recordPermissionDenied("SALE_CREATE", "SALE", null, "Order creation denied", "OWNER/MANAGER/STAFF");
            throw new RuntimeException(
                    "Current user cannot create orders"
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
}
