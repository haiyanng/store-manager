package com.storemanager.domain.onlineorder.presenter;

import com.storemanager.core.runtime.BaseModulePresenter;
import com.storemanager.core.runtime.async.AsyncTaskRunner;
import com.storemanager.domain.onlineorder.dao.OnlineOrderDataAccessException;
import com.storemanager.domain.onlineorder.model.OnlineOrderDetail;
import com.storemanager.domain.onlineorder.model.OnlineOrderSummary;
import com.storemanager.domain.onlineorder.service.OnlineOrderWorkflowService;
import com.storemanager.domain.onlineorder.view.OnlineOrderController;

import java.util.List;
import java.util.NoSuchElementException;

public class OnlineOrderPresenter extends BaseModulePresenter {

    private final OnlineOrderController view;

    private final OnlineOrderWorkflowService service = new OnlineOrderWorkflowService();

    public OnlineOrderPresenter(OnlineOrderController view) {
        this.view = view;
    }

    @Override
    public void initialize() {
        loadOrders(null);
    }

    public void loadOrders() {
        loadOrders(view.getSelectedOrderId());
    }

    private void loadOrders(Long preferredOrderId) {
        view.setBusy(true);
        view.setStatus("Loading online orders...");

        AsyncTaskRunner.run(
                () -> loadOrdersWithDetail(preferredOrderId),
                result -> {
                    view.setOrders(
                            result.orders(),
                            result.selectedOrderId()
                    );
                    if (result.detail() == null) {
                        view.clearDetail();
                        view.setStatus("No online orders found");
                        return;
                    }
                    view.showDetail(result.detail());
                    view.setStatus("Order #" + result.selectedOrderId() + " loaded");
                },
                throwable -> handleLoadFailure("online orders", throwable),
                () -> view.setBusy(false)
        );
    }

    public void loadOrderDetails(Long orderId) {
        if (orderId == null) {
            view.clearDetail();
            return;
        }

        view.setBusy(true);
        view.setStatus("Loading online order #" + orderId + "...");

        AsyncTaskRunner.run(
                () -> service.loadOrderDetails(orderId),
                detail -> {
                    view.showDetail(detail);
                    view.setStatus("Order #" + orderId + " loaded");
                },
                throwable -> handleDetailFailure(orderId, throwable),
                () -> view.setBusy(false)
        );
    }

    public void onOrderSelected(OnlineOrderSummary summary) {
        if (summary == null) {
            view.clearDetail();
            return;
        }
        loadOrderDetails(summary.getId());
    }

    public void refresh() {
        loadOrders();
    }

    public void updateStatus(
            Long orderId,
            String nextStatus,
            String note
    ) {
        if (orderId == null || nextStatus == null || nextStatus.isBlank()) {
            return;
        }

        view.setBusy(true);
        view.setStatus("Updating order #" + orderId + " to " + nextStatus + "...");

        AsyncTaskRunner.run(
                () -> updateStatusWithOrders(orderId, nextStatus, note),
                detail -> {
                    view.setOrders(
                            detail.orders(),
                            orderId
                    );
                    view.selectOrderById(orderId);
                    view.showDetail(detail.detail());
                    view.setStatus("Order #" + orderId + " updated to " + nextStatus);
                },
                throwable -> {
                    if (throwable instanceof IllegalStateException
                            && "Order status was changed by another user".equals(throwable.getMessage())) {
                        view.setStatus("Order status changed by another user. Refreshing...");
                        view.showError("Order status was changed by another user");
                        refresh();
                        return;
                    }
                    view.setStatus("Cannot update order status");
                    view.showError(detailMessage("online order status", throwable));
                },
                () -> view.setBusy(false)
        );
    }

    private OrdersLoadResult loadOrdersWithDetail(
            Long preferredOrderId
    ) {

        List<OnlineOrderSummary> orders =
                service.loadAllOrders();

        if (orders.isEmpty()) {
            return new OrdersLoadResult(
                    orders,
                    null,
                    null
            );
        }

        Long selectedOrderId =
                resolveSelectedOrderId(
                        orders,
                        preferredOrderId
                );

        return new OrdersLoadResult(
                orders,
                selectedOrderId,
                service.loadOrderDetails(selectedOrderId)
        );
    }

    private StatusUpdateResult updateStatusWithOrders(
            Long orderId,
            String nextStatus,
            String note
    ) {

        OnlineOrderDetail detail =
                service.updateStatus(
                        orderId,
                        nextStatus,
                        note
                );

        return new StatusUpdateResult(
                service.loadAllOrders(),
                detail
        );
    }

    private Long resolveSelectedOrderId(
            List<OnlineOrderSummary> orders,
            Long preferredOrderId
    ) {

        if (preferredOrderId != null) {
            for (OnlineOrderSummary order : orders) {
                if (preferredOrderId.equals(order.getId())) {
                    return preferredOrderId;
                }
            }
        }

        return orders.get(0).getId();
    }

    private void handleLoadFailure(String scope, Throwable throwable) {
        view.setStatus("Cannot load " + scope);
        view.showError(detailMessage(scope, throwable));
    }

    private void handleDetailFailure(Long orderId, Throwable throwable) {
        if (throwable instanceof NoSuchElementException) {
            view.clearDetail();
            view.setStatus("Online order not found");
            view.showError("Online order not found");
            return;
        }
        view.setStatus("Cannot load online order details");
        view.showError(detailMessage("online order details", throwable));
    }

    private String detailMessage(String scope, Throwable throwable) {
        if (throwable instanceof OnlineOrderDataAccessException) {
            return "Database failure while loading " + scope;
        }
        String message = throwable == null ? null : throwable.getMessage();
        return message == null || message.isBlank() ? "Request failed" : message;
    }

    private record OrdersLoadResult(
            List<OnlineOrderSummary> orders,
            Long selectedOrderId,
            OnlineOrderDetail detail
    ) {
    }

    private record StatusUpdateResult(
            List<OnlineOrderSummary> orders,
            OnlineOrderDetail detail
    ) {
    }
}
