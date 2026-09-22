package com.storemanager.domain.onlineorder.service;

import com.storemanager.core.database.ConnectionFactory;
import com.storemanager.core.security.PermissionGuard;
import com.storemanager.domain.audit.service.AuditService;
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

import java.sql.Connection;
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

    private final AuditService auditService =
            new AuditService();

    public List<OnlineOrderSummary> loadAllOrders() {
        validateOnlineOrderViewAccess();
        return readService.loadAllOrders();
    }

    public OnlineOrderDetail loadOrderDetails(Long orderId) {
        validateOnlineOrderViewAccess();
        return readService.loadOrderDetails(orderId);
    }

    public OnlineOrderDetail updateStatus(
            Long orderId,
            String nextStatus,
            String note
    ) {

        validateOnlineOrderActionAccess(nextStatus);

        OnlineOrderDetail detail =
                loadOrderDetails(orderId);

        OnlineOrder order =
                detail.getOrder();

        String currentStatus =
                order.getStatus();

        String cleanedNote =
                clean(note);

        statusService.validateTransition(
                currentStatus,
                nextStatus
        );

        validateActionNote(
                nextStatus,
                cleanedNote
        );

        if (isManualHandlingRequired(
                currentStatus,
                nextStatus
        )) {
            throw new IllegalStateException(
                    MANUAL_CANCEL_MESSAGE
            );
        }

        try {
            if (OnlineOrderStatus.CONFIRMED.equals(nextStatus)) {
                applyConfirmWorkflow(
                        order,
                        detail.getItems(),
                        currentStatus,
                        nextStatus,
                        cleanedNote
                );
            } else if (requiresRestockTransition(
                    currentStatus,
                    nextStatus
            )) {
                applyRestockWorkflow(
                        order,
                        detail.getItems(),
                        currentStatus,
                        nextStatus,
                        cleanedNote
                );
            } else {
                updateStatusOnly(
                        order,
                        currentStatus,
                        nextStatus,
                        cleanedNote
                );
            }
        } catch (RuntimeException e) {
            auditService.recordEvent(
                    "ONLINE_ORDER",
                    actionForStatus(nextStatus),
                    "ONLINE_ORDER",
                    orderId,
                    false,
                    rootMessage(e),
                    detailsJson(currentStatus, nextStatus, cleanedNote)
            );
            throw e;
        }

        order.setStatus(nextStatus);
        detail.setHistory(
                onlineOrderDAO.findHistoryByOrderId(orderId)
        );
        auditService.recordEvent(
                "ONLINE_ORDER",
                actionForStatus(nextStatus),
                "ONLINE_ORDER",
                orderId,
                true,
                null,
                detailsJson(currentStatus, nextStatus, cleanedNote)
        );
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
            String nextStatus,
            String note
    ) {

        applyInventoryAndStatusAtomically(
                order,
                items,
                currentStatus,
                nextStatus,
                note,
                InventoryTransactionType.SALE,
                withNote(
                        "Online order confirmed, orderId=" + order.getId(),
                        note
                )
        );
    }

    private void applyRestockWorkflow(
            OnlineOrder order,
            List<OnlineOrderItem> items,
            String currentStatus,
            String nextStatus,
            String note
    ) {

        applyInventoryAndStatusAtomically(
                order,
                items,
                currentStatus,
                nextStatus,
                note,
                InventoryTransactionType.ADJUSTMENT,
                withNote(
                        "Online order restocked, orderId=" + order.getId(),
                        note
                )
        );
    }

    private void applyInventoryAndStatusAtomically(
            OnlineOrder order,
            List<OnlineOrderItem> items,
            String currentStatus,
            String nextStatus,
            String note,
            InventoryTransactionType transactionType,
            String reason
    ) {

        try (
                Connection connection =
                        ConnectionFactory.getConnection()
        ) {
            connection.setAutoCommit(false);

            try {
                for (OnlineOrderItem item : items) {
                    InventoryTransaction transaction =
                            new InventoryTransaction();

                    transaction.setProductId(
                            item.getProductId()
                    );
                    transaction.setType(transactionType);
                    transaction.setQuantity(item.getQuantity());
                    transaction.setReason(reason);

                    boolean success =
                            inventoryService.adjustStock(
                                    connection,
                                    transaction
                            );

                    if (!success) {
                        throw new IllegalStateException(
                                "Cannot update inventory for online order "
                                        + order.getId()
                                        + ", productId="
                                        + item.getProductId()
                        );
                    }
                }

                boolean updated =
                        onlineOrderDAO.updateStatus(
                                connection,
                                order.getId(),
                                currentStatus,
                                nextStatus,
                                actionForStatus(nextStatus),
                                note
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

                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    "Cannot update online order atomically",
                    e
            );
        }
    }

    private void updateStatusOnly(
            OnlineOrder order,
            String currentStatus,
            String nextStatus,
            String note
    ) {

        boolean updated =
                onlineOrderDAO.updateStatus(
                        order.getId(),
                        currentStatus,
                        nextStatus,
                        actionForStatus(nextStatus),
                        note
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

    private void validateOnlineOrderViewAccess() {

        if (!PermissionGuard.canViewOnlineOrders()) {
            auditService.recordPermissionDenied(
                    "ONLINE_ORDER_VIEW",
                    "ONLINE_ORDER",
                    null,
                    "Online order access denied",
                    null,
                    "OWNER/MANAGER"
            );
            throw new RuntimeException(
                    "Online order access denied"
            );
        }
    }

    private void validateOnlineOrderActionAccess(
            String nextStatus
    ) {

        boolean allowed =
                ifRefund(nextStatus)
                        ? PermissionGuard.canRefundOnlineOrder()
                        : ifCancel(nextStatus)
                        ? PermissionGuard.canCancelOnlineOrder()
                        : PermissionGuard.canModifyOnlineOrders();

        if (!allowed) {
            auditService.recordPermissionDenied(
                    actionForStatus(nextStatus),
                    "ONLINE_ORDER",
                    null,
                    "Online order action denied",
                    null,
                    "OWNER/MANAGER"
            );
            throw new RuntimeException(
                    "Current user cannot update online orders"
            );
        }
    }

    private boolean ifCancel(
            String status
    ) {

        return OnlineOrderStatus.CANCELLED.equals(status)
                || OnlineOrderStatus.REJECTED.equals(status);
    }

    private boolean ifRefund(
            String status
    ) {

        return OnlineOrderStatus.REFUNDED.equals(status);
    }

    private boolean requiresRestockTransition(
            String currentStatus,
            String nextStatus
    ) {

        boolean stockWasReserved =
                OnlineOrderStatus.CONFIRMED.equals(currentStatus)
                        || OnlineOrderStatus.PREPARING.equals(currentStatus)
                        || OnlineOrderStatus.READY.equals(currentStatus)
                        || OnlineOrderStatus.DELIVERING.equals(currentStatus)
                        || OnlineOrderStatus.DELIVERED.equals(currentStatus)
                        || OnlineOrderStatus.COMPLETED.equals(currentStatus);

        return stockWasReserved
                && (OnlineOrderStatus.CANCELLED.equals(nextStatus)
                || OnlineOrderStatus.REJECTED.equals(nextStatus)
                || OnlineOrderStatus.REFUNDED.equals(nextStatus));
    }

    private boolean isManualHandlingRequired(
            String currentStatus,
            String nextStatus
    ) {

        return OnlineOrderStatus.DELIVERING.equals(currentStatus)
                && OnlineOrderStatus.CANCELLED.equals(nextStatus);
    }

    private void validateActionNote(
            String nextStatus,
            String note
    ) {

        if ((OnlineOrderStatus.CANCELLED.equals(nextStatus)
                || OnlineOrderStatus.REJECTED.equals(nextStatus))
                && (note == null || note.isBlank())) {
            throw new IllegalArgumentException(
                    "A note is required for this action"
            );
        }
    }

    private String actionForStatus(
            String nextStatus
    ) {

        if (OnlineOrderStatus.CONFIRMED.equals(nextStatus)) {
            return "CONFIRM";
        }
        if (OnlineOrderStatus.CANCELLED.equals(nextStatus)) {
            return "CANCEL";
        }
        if (OnlineOrderStatus.REFUNDED.equals(nextStatus)) {
            return "REFUND";
        }
        if (OnlineOrderStatus.REJECTED.equals(nextStatus)) {
            return "REJECT";
        }

        return nextStatus;
    }

    private String withNote(
            String reason,
            String note
    ) {

        if (note == null || note.isBlank()) {
            return reason;
        }

        return reason + ", note=" + note;
    }

    private String clean(
            String value
    ) {

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
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

    private String detailsJson(
            String currentStatus,
            String nextStatus,
            String note
    ) {

        return "{\"old_status\":\"" + escape(currentStatus)
                + "\",\"new_status\":\"" + escape(nextStatus)
                + "\",\"note\":\"" + escape(note) + "\"}";
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
