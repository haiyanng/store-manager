package com.customershopfx.cart.presenter;

import com.customershopfx.cart.model.Cart;
import com.customershopfx.cart.model.CartItem;
import com.customershopfx.cart.service.CartApiService;
import com.customershopfx.cart.viewmodel.CartViewModel;
import com.customershopfx.shop.view.ShopController;
import javafx.concurrent.Task;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class CartPresenter {
    private final ShopController view;
    private final CartViewModel viewModel;
    private final CartApiService cartApiService;
    private final Consumer<String> statusSink;

    public CartPresenter(ShopController view, CartViewModel viewModel, Consumer<String> statusSink) {
        this.view = view;
        this.viewModel = viewModel;
        this.statusSink = statusSink;
        this.cartApiService = new CartApiService();
    }

    public void initialize() {
        loadCart();
    }

    public void loadCart() {
        viewModel.markLoading();
        statusSink.accept("Loading cart...");
        run(cartApiService::cart, cart -> {
            apply(cart);
            view.renderCart(cart);
            viewModel.markReady();
            statusSink.accept("Ready");
        }, null);
    }

    public void addItem(Long productId, int quantity) {
        addItem(productId, quantity, null, null);
    }

    public void addItem(Long productId, int quantity, Runnable onSuccess, Consumer<String> onError) {
        viewModel.markLoading();
        statusSink.accept("Updating cart...");
        run(() -> cartApiService.add(productId, quantity), cart -> {
            apply(cart);
            view.renderCart(cart);
            viewModel.markReady();
            statusSink.accept("Cart updated");
            if (onSuccess != null) {
                onSuccess.run();
            }
        }, onError);
    }

    public void updateQuantity(Long itemId, int quantity) {
        viewModel.markLoading();
        statusSink.accept("Updating cart...");
        run(() -> cartApiService.update(itemId, quantity), cart -> {
            apply(cart);
            view.renderCart(cart);
            viewModel.markReady();
            statusSink.accept("Cart updated");
        }, null);
    }

    public void removeItem(Long itemId) {
        viewModel.markLoading();
        statusSink.accept("Updating cart...");
        run(() -> cartApiService.remove(itemId), cart -> {
            apply(cart);
            view.renderCart(cart);
            viewModel.markReady();
            statusSink.accept("Cart updated");
        }, null);
    }

    public void clearCart() {
        viewModel.markLoading();
        statusSink.accept("Clearing cart...");
        run(cartApiService::clear, cart -> {
            apply(cart);
            view.renderCart(cart);
            viewModel.markReady();
            statusSink.accept("Cart cleared");
        }, null);
    }

    public CartViewModel viewModel() {
        return viewModel;
    }

    private void apply(Cart cart) {
        viewModel.setCart(cart);
        if (cart != null) {
            viewModel.setItems(cart.items());
            viewModel.setTotal(cart.total());
            viewModel.setItemCount(cart.items().stream().mapToInt(CartItem::quantity).sum());
        }
    }

    private <T> void run(Callable<T> callable, Consumer<T> success, Consumer<String> failure) {
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
                String message = message(ex);
                viewModel.markFailure(message);
                statusSink.accept(message);
                if (failure != null) {
                    failure.accept(message);
                }
            }
        });
        task.setOnFailed(event -> {
            String message = message(task.getException());
            viewModel.markFailure(message);
            statusSink.accept(message);
            if (failure != null) {
                failure.accept(message);
            }
        });
        Thread thread = new Thread(task, "cart-api-task");
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
