package com.customershopfx.product.presenter;

import com.customershopfx.product.model.Category;
import com.customershopfx.product.model.Product;
import com.customershopfx.cart.presenter.CartPresenter;
import com.customershopfx.product.service.ProductApiService;
import com.customershopfx.product.viewmodel.ProductViewModel;
import com.customershopfx.shop.view.ShopController;
import javafx.concurrent.Task;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class ProductPresenter {
    private final ShopController view;
    private final ProductViewModel viewModel;
    private final ProductApiService productApiService;
    private final CartPresenter cartPresenter;
    private final Consumer<String> statusSink;

    public ProductPresenter(ShopController view, ProductViewModel viewModel, Consumer<String> statusSink,
                            CartPresenter cartPresenter) {
        this.view = view;
        this.viewModel = viewModel;
        this.productApiService = new ProductApiService();
        this.statusSink = statusSink;
        this.cartPresenter = cartPresenter;
    }

    public void initialize() {
        loadCategories();
        loadProducts();
    }

    public void loadProducts() {
        viewModel.markLoading();
        statusSink.accept("Loading products...");
        run(() -> productApiService.products(viewModel.getKeyword(),
                selectedCategoryId(), viewModel.getSelectedSort()),
                products -> {
                    viewModel.setProducts(products);
                    view.renderProducts(products);
                    viewModel.markReady();
                    statusSink.accept(products.isEmpty() ? "No products found" : products.size() + " products");
                });
    }

    public void loadCategories() {
        viewModel.markLoading();
        statusSink.accept("Loading categories...");
        run(productApiService::categories, categories -> {
            viewModel.setCategories(categories);
            view.renderCategories(categories);
            viewModel.markReady();
            statusSink.accept("Ready");
        });
    }

    public void refreshCatalog() {
        loadCategories();
        loadProducts();
    }

    public void addToCart(Long productId, int quantity) {
        addToCart(productId, quantity, "Item");
    }

    public void addToCart(Long productId, int quantity, String productName) {
        statusSink.accept("Adding...");
        cartPresenter.addItem(productId, quantity,
                () -> view.showCartNotificationSuccess(productName, quantity),
                message -> view.showToastError("Could not add to cart. Please try again."));
    }

    public void showProductDetail(Product product) {
        view.renderProductDetail(product);
    }

    public ProductViewModel viewModel() {
        return viewModel;
    }

    private Long selectedCategoryId() {
        Category selected = viewModel.getSelectedCategory();
        return selected == null ? null : selected.id();
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
        Thread thread = new Thread(task, "product-api-task");
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
