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
        loadOrders();
    }

    public void loadOrders() {
        view.setBusy(true);
        view.setStatus("Loading online orders...");

        AsyncTaskRunner.run(
                service::loadAllOrders,
                orders -> {
                    view.setOrders(orders);
                    view.setStatus(orders.isEmpty() ? "No online orders found" : "Ready");
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

    public void updateStatus(Long orderId, String nextStatus) {
        if (orderId == null || nextStatus == null || nextStatus.isBlank()) {
            return;
        }

        view.setBusy(true);
        view.setStatus("Updating order #" + orderId + " to " + nextStatus + "...");

        AsyncTaskRunner.run(
                () -> service.updateStatus(orderId, nextStatus),
                detail -> {
                    try {
                        view.setOrders(service.loadAllOrders());
                    } catch (Throwable refreshError) {
                        view.showError(detailMessage("online order list", refreshError));
                    }
                    view.selectOrderById(orderId);
                    view.showDetail(detail);
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
}
