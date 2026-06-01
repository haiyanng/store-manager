package com.storemanager.domain.onlineorder.service;

import com.storemanager.domain.inventory.model.InventoryItem;
import com.storemanager.domain.inventory.model.InventoryTransaction;
import com.storemanager.domain.inventory.model.InventoryTransactionType;
import com.storemanager.domain.inventory.service.InventoryService;
import com.storemanager.domain.onlineorder.dao.OnlineOrderDAO;
import com.storemanager.domain.onlineorder.model.OnlineOrder;
import com.storemanager.domain.onlineorder.model.OnlineOrderDetail;
import com.storemanager.domain.onlineorder.model.OnlineOrderItem;
import com.storemanager.domain.onlineorder.model.OnlineOrderStatus;
import com.storemanager.domain.onlineorder.model.OnlineOrderSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class OnlineOrderWorkflowService {

    private static final Logger log =
            Logger.getLogger(OnlineOrderWorkflowService.class.getName());

    private static final String CONFLICT_MESSAGE =
            "Order status was changed by another user";

    private static final String MANUAL_CANCEL_MESSAGE =
            "Order is being delivered and needs manual handling";

    private final OnlineOrderService readService =
            new OnlineOrderService();

    private final OnlineOrderDAO onlineOrderDAO =
            new OnlineOrderDAO();

    private final OnlineOrderStatusService statusService =
            new OnlineOrderStatusService();

    private final InventoryService inventoryService =
            new InventoryService();

    public List<OnlineOrderSummary> loadAllOrders() {
        return readService.loadAllOrders();
    }

    public OnlineOrderDetail loadOrderDetails(Long orderId) {
        return readService.loadOrderDetails(orderId);
    }

    public OnlineOrderDetail updateStatus(
            Long orderId,
            String nextStatus
    ) {

        OnlineOrderDetail detail =
                loadOrderDetails(orderId);

        OnlineOrder order =
                detail.getOrder();

        String currentStatus =
                order.getStatus();

        statusService.validateTransition(
                currentStatus,
                nextStatus
        );

        if (isManualHandlingRequired(
                currentStatus,
                nextStatus
        )) {
            throw new IllegalStateException(
                    MANUAL_CANCEL_MESSAGE
            );
        }

        if (OnlineOrderStatus.CONFIRMED.equals(nextStatus)) {
            applyConfirmWorkflow(
                    order,
                    detail.getItems(),
                    currentStatus,
                    nextStatus
            );
        } else if (OnlineOrderStatus.CANCELLED.equals(nextStatus)
                && requiresRestock(
                currentStatus
        )) {
            applyCancelWorkflow(
                    order,
                    detail.getItems(),
                    currentStatus,
                    nextStatus
            );
        } else {
            updateStatusOnly(
                    order,
                    currentStatus,
                    nextStatus
            );
        }

        order.setStatus(nextStatus);
        log.info(() -> "Online order workflow completed orderId="
                + orderId
                + ", oldStatus="
                + currentStatus
                + ", newStatus="
                + nextStatus);
        return detail;
    }

    private void applyConfirmWorkflow(
            OnlineOrder order,
            List<OnlineOrderItem> items,
            String currentStatus,
            String nextStatus
    ) {

        ensureStockAvailable(items);

        List<OnlineOrderItem> appliedItems =
                applyInventoryChangesWithRollback(
                order.getId(),
                items,
                InventoryTransactionType.SALE,
                "Online order confirmed, orderId=" + order.getId(),
                InventoryTransactionType.ADJUSTMENT
        );

        try {
            updateStatusOnly(
                    order,
                    currentStatus,
                    nextStatus
            );
        } catch (RuntimeException e) {
            rollbackInventoryChanges(
                    order.getId(),
                    appliedItems,
                    InventoryTransactionType.ADJUSTMENT
            );
            throw e;
        }
    }

    private void applyCancelWorkflow(
            OnlineOrder order,
            List<OnlineOrderItem> items,
            String currentStatus,
            String nextStatus
    ) {

        List<OnlineOrderItem> appliedItems =
                applyInventoryChangesWithRollback(
                order.getId(),
                items,
                InventoryTransactionType.ADJUSTMENT,
                "Online order cancelled/restocked, orderId=" + order.getId(),
                InventoryTransactionType.SALE
        );

        try {
            updateStatusOnly(
                    order,
                    currentStatus,
                    nextStatus
            );
        } catch (RuntimeException e) {
            rollbackInventoryChanges(
                    order.getId(),
                    appliedItems,
                    InventoryTransactionType.SALE
            );
            throw e;
        }
    }

    private void updateStatusOnly(
            OnlineOrder order,
            String currentStatus,
            String nextStatus
    ) {

        boolean updated =
                onlineOrderDAO.updateStatus(
                        order.getId(),
                        currentStatus,
                        nextStatus
                );

        if (!updated) {
            log.warning(() -> "Online order status update rejected orderId="
                    + order.getId()
                    + ", oldStatus="
                    + currentStatus
                    + ", newStatus="
                    + nextStatus
                    + ", result=conflict");
            throw new IllegalStateException(
                    CONFLICT_MESSAGE
            );
        }
    }

    private void ensureStockAvailable(
            List<OnlineOrderItem> items
    ) {

        Map<Long, Integer> stockByProductId =
                inventoryService.findAllItems()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        InventoryItem::getProductId,
                                        InventoryItem::getQuantity,
                                        Integer::sum
                                )
                        );

        for (OnlineOrderItem item : items) {
            Long productId =
                    item.getProductId();
            int currentQuantity =
                    stockByProductId.getOrDefault(
                            productId,
                            0
                    );

            if (currentQuantity < item.getQuantity()) {
                throw new IllegalStateException(
                        "Insufficient stock for "
                                + safeProductName(item)
                );
            }
        }
    }

    private List<OnlineOrderItem> applyInventoryChangesWithRollback(
            Long orderId,
            List<OnlineOrderItem> items,
            InventoryTransactionType applyType,
            String reason,
            InventoryTransactionType rollbackType
    ) {

        List<OnlineOrderItem> appliedItems =
                new ArrayList<>();

        try {
            for (OnlineOrderItem item : items) {
                adjustInventory(
                        orderId,
                        item,
                        applyType,
                        reason
                );
                appliedItems.add(item);
            }
        } catch (RuntimeException e) {
            rollbackInventoryChanges(
                    orderId,
                    appliedItems,
                    rollbackType
            );
            throw e;
        }

        return appliedItems;
    }

    private void rollbackInventoryChanges(
            Long orderId,
            List<OnlineOrderItem> appliedItems,
            InventoryTransactionType rollbackType
    ) {

        if (appliedItems == null || appliedItems.isEmpty()) {
            return;
        }

        for (int index = appliedItems.size() - 1; index >= 0; index--) {
            OnlineOrderItem item =
                    appliedItems.get(index);
            try {
                adjustInventory(
                        orderId,
                        item,
                        rollbackType,
                        "Rollback online order inventory, orderId=" + orderId
                );
            } catch (RuntimeException rollbackError) {
                log.severe(() -> "Cannot rollback online order inventory orderId="
                        + orderId
                        + ", productId="
                        + item.getProductId()
                        + ", error="
                        + rollbackError.getMessage());
                throw rollbackError;
            }
        }
    }

    private void adjustInventory(
            Long orderId,
            OnlineOrderItem item,
            InventoryTransactionType type,
            String reason
    ) {

        InventoryTransaction transaction =
                new InventoryTransaction();

        transaction.setProductId(
                item.getProductId()
        );
        transaction.setType(type);
        transaction.setQuantity(item.getQuantity());
        transaction.setReason(reason);

        boolean success =
                inventoryService.adjustStock(transaction);

        if (!success) {
            throw new IllegalStateException(
                    "Cannot update inventory for online order "
                            + orderId
                            + ", productId="
                            + item.getProductId()
            );
        }
    }

    private boolean requiresRestock(
            String currentStatus
    ) {

        return OnlineOrderStatus.CONFIRMED.equals(currentStatus)
                || OnlineOrderStatus.PREPARING.equals(currentStatus);
    }

    private boolean isManualHandlingRequired(
            String currentStatus,
            String nextStatus
    ) {

        return OnlineOrderStatus.DELIVERING.equals(currentStatus)
                && OnlineOrderStatus.CANCELLED.equals(nextStatus);
    }

    private String safeProductName(
            OnlineOrderItem item
    ) {

        if (item == null || item.getProductName() == null
                || item.getProductName().isBlank()) {
            return "Product #" + (item == null ? "-" : item.getProductId());
        }

        return item.getProductName();
    }
}
