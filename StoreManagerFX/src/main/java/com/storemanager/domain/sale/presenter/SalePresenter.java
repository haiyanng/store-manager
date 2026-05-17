package com.storemanager.domain.sale.presenter;

import com.storemanager.core.runtime.BaseModulePresenter;
import com.storemanager.core.runtime.async.AsyncTaskRunner;
import com.storemanager.core.runtime.async.LoadingState;
import com.storemanager.domain.product.model.Product;
import com.storemanager.domain.sale.model.SaleCartItem;
import com.storemanager.domain.sale.model.SaleOrder;
import com.storemanager.domain.sale.service.SaleService;
import com.storemanager.domain.sale.view.SaleController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SalePresenter extends BaseModulePresenter {

    private final SaleController view;

    private final SaleService saleService =
            new SaleService();

    private final List<SaleCartItem> cartItems =
            new ArrayList<>();

    private LoadingState loadingState =
            LoadingState.IDLE;

    public SalePresenter(
            SaleController view
    ) {

        this.view = view;
    }

    @Override
    public void initialize() {

        loadSaleData();
        updateCartView();
    }

    public void loadSaleData() {

        loadingState =
                LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus("Loading sale workspace...");

        AsyncTaskRunner.run(
                () -> new SaleData(
                        saleService.findProducts(),
                        saleService.findQuickPickProducts(20),
                        saleService.findRecentOrders()
                ),
                data -> {
                    view.setProducts(data.products());
                    view.setQuickPickProducts(data.quickPickProducts());
                    view.setRecentOrders(data.orders());
                    loadingState =
                            LoadingState.SUCCESS;
                    view.setStatus("Ready");
                },
                throwable -> {
                    loadingState =
                            LoadingState.ERROR;
                    view.setStatus("Cannot load sale workspace");
                    view.showError(throwable.getMessage());
                },
                () -> view.setBusy(false)
        );
    }

    public void addProductToCart(
            Product product,
            int quantity
    ) {

        try {

            if (product == null) {
                throw new RuntimeException(
                        "Product is required"
                );
            }

            if (quantity <= 0) {
                throw new RuntimeException(
                        "Quantity must be positive"
                );
            }

            SaleCartItem existingItem =
                    findCartItem(product);

            if (existingItem == null) {
                cartItems.add(
                        new SaleCartItem(
                                product,
                                quantity
                        )
                );
            } else {
                existingItem.setQuantity(
                        existingItem.getQuantity() + quantity
                );
            }

            view.clearEntryForm();
            updateCartView();

        } catch (Exception e) {

            view.showError(e.getMessage());
        }
    }

    public void removeCartItem(
            SaleCartItem item
    ) {

        if (item == null) {
            view.showError("Select a cart item to remove");
            return;
        }

        cartItems.remove(item);
        updateCartView();
    }

    public void clearCart() {

        cartItems.clear();
        view.clearEntryForm();
        updateCartView();
    }

    public void finalizeSale() {

        if (cartItems.isEmpty()) {
            view.showError("Cart is empty");
            return;
        }

        loadingState =
                LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus("Finalizing sale...");

        List<SaleCartItem> saleItems =
                new ArrayList<>(cartItems);

        AsyncTaskRunner.run(
                () -> saleService.finalizeSale(saleItems),
                orderId -> {
                    cartItems.clear();
                    view.clearEntryForm();
                    updateCartView();
                    view.setStatus("Sale order #" + orderId + " created");
                    loadSaleData();
                },
                throwable -> {
                    loadingState =
                            LoadingState.ERROR;
                    view.setStatus("Cannot finalize sale");
                    view.showError(throwable.getMessage());
                    view.setBusy(false);
                },
                null
        );
    }

    public LoadingState getLoadingState() {
        return loadingState;
    }

    private SaleCartItem findCartItem(
            Product product
    ) {

        return cartItems
                .stream()
                .filter(item ->
                        item.getProduct() != null
                                && product.getId().equals(
                                item.getProduct().getId()
                        )
                )
                .findFirst()
                .orElse(null);
    }

    private void updateCartView() {

        view.setCartItems(
                new ArrayList<>(cartItems)
        );

        BigDecimal total =
                saleService.calculateTotal(cartItems);

        view.setTotalAmount(total);
    }

    private record SaleData(
            List<Product> products,
            List<Product> quickPickProducts,
            List<SaleOrder> orders
    ) {
    }
}
