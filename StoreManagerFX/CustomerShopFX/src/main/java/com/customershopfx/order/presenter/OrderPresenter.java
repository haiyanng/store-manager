package com.customershopfx.order.presenter;

import com.customershopfx.app.SessionManager;
import com.customershopfx.order.model.Order;
import com.customershopfx.order.service.OrderApiService;
import com.customershopfx.order.viewmodel.OrderViewModel;
import com.customershopfx.cart.presenter.CartPresenter;
import com.customershopfx.shop.view.ShopController;
import javafx.concurrent.Task;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class OrderPresenter {
    private final ShopController view;
    private final OrderViewModel viewModel;
    private final OrderApiService orderApiService;
    private final CartPresenter cartPresenter;
    private final Consumer<String> statusSink;

    public OrderPresenter(ShopController view, OrderViewModel viewModel, Consumer<String> statusSink,
                          CartPresenter cartPresenter) {
        this.view = view;
        this.viewModel = viewModel;
        this.statusSink = statusSink;
        this.cartPresenter = cartPresenter;
        this.orderApiService = new OrderApiService();
    }

    public void initialize() {
        if (SessionManager.customer() != null) {
            viewModel.setCheckoutName(SessionManager.customer().fullName());
            viewModel.setCheckoutPhone(SessionManager.customer().phone());
        }
    }

    public void loadOrders() {
        viewModel.markLoading();
        statusSink.accept("Loading orders...");
        run(orderApiService::myOrders, orders -> {
            viewModel.setOrders(orders);
            viewModel.markReady();
            statusSink.accept(orders.isEmpty() ? "No orders yet" : "Orders loaded");
        });
    }

    public void placeOrder() {
        viewModel.markLoading();
        statusSink.accept("Placing order...");
        run(() -> orderApiService.create(
                viewModel.getCheckoutName(),
                viewModel.getCheckoutPhone(),
                viewModel.getCheckoutAddress(),
                viewModel.getPaymentMethod()), order -> {
            viewModel.setCheckoutMessage("Order #" + order.id() + " placed successfully");
            viewModel.setSelectedOrder(order);
            cartPresenter.loadCart();
            loadOrders();
            view.renderOrderDetail(order);
            viewModel.markReady();
            statusSink.accept("Order placed");
        });
    }

    public void loadOrderDetail(Long id) {
        viewModel.markLoading();
        statusSink.accept("Loading order...");
        run(() -> orderApiService.detail(id), order -> {
            viewModel.setSelectedOrder(order);
            view.renderOrderDetail(order);
            viewModel.markReady();
            statusSink.accept("Order #" + order.id() + " loaded");
        });
    }

    public void cancelOrder(Long id) {
        viewModel.markLoading();
        statusSink.accept("Cancelling order...");
        run(() -> orderApiService.cancel(id), order -> {
            viewModel.setSelectedOrder(order);
            view.renderOrderDetail(order);
            loadOrders();
            viewModel.markReady();
            statusSink.accept("Order #" + order.id() + " cancelled");
        });
    }

    public void onOrderSelected(Order order) {
        if (order != null) {
            loadOrderDetail(order.id());
        }
    }

    public void syncCheckout(String name, String phone, String address, String paymentMethod) {
        viewModel.setCheckoutName(name);
        viewModel.setCheckoutPhone(phone);
        viewModel.setCheckoutAddress(address);
        viewModel.setPaymentMethod(paymentMethod);
    }

    public OrderViewModel viewModel() {
        return viewModel;
    }

    private <T> void run(Callable<T> callable, Consumer<T> success) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return callable.call();
            }
        };
        task.setOnSucceeded(event -> {
            try {
                success.accept(task.getValue());
            } catch (RuntimeException ex) {
                viewModel.markFailure(message(ex));
                statusSink.accept(message(ex));
            }
        });
        task.setOnFailed(event -> {
            String message = message(task.getException());
            viewModel.markFailure(message);
            statusSink.accept(message);
        });
        Thread thread = new Thread(task, "order-api-task");
        thread.setDaemon(true);
        thread.start();
    }

    private String message(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && (current.getMessage() == null || current.getMessage().isBlank()) && current.getCause() != null) {
            current = current.getCause();
        }
        String message = current == null ? null : current.getMessage();
        return message == null || message.isBlank() ? "Request failed" : message;
    }
}
