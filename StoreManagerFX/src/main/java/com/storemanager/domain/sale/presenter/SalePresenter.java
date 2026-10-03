package com.storemanager.domain.sale.presenter;

import com.storemanager.core.runtime.BaseModulePresenter;
import com.storemanager.core.runtime.async.AsyncTaskRunner;
import com.storemanager.core.runtime.async.LoadingState;
import com.storemanager.domain.product.model.Product;
import com.storemanager.domain.sale.model.SaleCartItem;
import com.storemanager.domain.sale.model.SaleOrder;
import com.storemanager.domain.sale.model.SelectedProductPreviewDto;
import com.storemanager.domain.sale.service.SaleService;
import com.storemanager.domain.sale.view.SaleController;
import com.storemanager.core.util.MoneyFormatUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SalePresenter extends BaseModulePresenter {

    private final SaleController view;

    private final SaleService saleService =
            new SaleService();

    private final List<SaleCartItem> cartItems =
            new ArrayList<>();

    private Map<Long, Integer> stockByProductId =
            Map.of();

    private LoadingState loadingState =
            LoadingState.IDLE;

    public SalePresenter(
            SaleController view
    ) {

        this.view = view;
    }

    @Override
    public void initialize() {

        view.clearSelectedProductPreview();
        loadSaleData();
        updateCartView();
    }

    public void loadSaleData() {

        if (loadingState == LoadingState.LOADING) return;

        loadingState =
                LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus("Loading order workspace...");
        view.clearSelectedProductPreview();

        AsyncTaskRunner.run(
                () -> new SaleData(
                        saleService.findProducts(),
                        saleService.findQuickPickProducts(20),
                        saleService.findRecentOrders(),
                        saleService.findCurrentStockByProductId()
                ),
                data -> {
                    stockByProductId =
                            data.stockByProductId();
                    view.setProducts(data.products());
                    view.setQuickPickProducts(data.quickPickProducts());
                    view.setRecentOrders(data.orders());
                    view.setOrderItemDetails(List.of());
                    loadingState =
                            LoadingState.SUCCESS;
                    view.setStatus("Ready");
                },
                throwable -> {
                    loadingState =
                            LoadingState.ERROR;
                    view.setStatus("Unable to load order workspace");
                    view.showError(throwable.getMessage());
                },
                () -> view.setBusy(false)
        );
    }

    public void selectOrder(
            SaleOrder order
    ) {

        if (order == null) {
            view.setOrderItemDetails(List.of());
            view.setStatus("Select an order to view details");
            return;
        }

        loadingState =
                LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus("Loading order #" + order.getId() + " details...");

        AsyncTaskRunner.run(
                () -> saleService.findOrderItemDetails(order),
                details -> {
                    view.setOrderItemDetails(details);
                    loadingState =
                            LoadingState.SUCCESS;
                    view.setStatus("Order #" + order.getId() + " details loaded");
                },
                throwable -> {
                    loadingState =
                            LoadingState.ERROR;
                    view.setStatus("Unable to load order details");
                    view.showError(throwable.getMessage());
                },
                () -> view.setBusy(false)
        );
    }

    public void addProductToCart(
            Product product,
            int quantity
    ) {

        if (loadingState == LoadingState.LOADING) return;

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
                        Math.addExact(existingItem.getQuantity(), quantity)
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

        if (loadingState == LoadingState.LOADING) return;

        if (item == null) {
            view.showError("Select a cart item to remove");
            return;
        }

        cartItems.remove(item);
        updateCartView();
    }

    public void clearCart() {

        if (loadingState == LoadingState.LOADING) return;

        cartItems.clear();
        view.resetPayment();
        view.clearEntryForm();
        updateCartView();
    }

    public void updateQuantity(SaleCartItem item, int quantity) {
        if (loadingState == LoadingState.LOADING) return;
        if (item == null || !cartItems.contains(item)) {
            view.showError("Select a cart item to update");
            return;
        }
        if (quantity <= 0) {
            view.showError("Quantity must be positive");
            return;
        }
        item.setQuantity(quantity);
        updateCartView();
    }

    public void updatePayment(String text) {
        if (text == null || text.isBlank()) {
            view.setChangeText("-");
            return;
        }
        try {
            BigDecimal change = SaleService.parseAmountReceived(text).subtract(saleService.calculateTotal(cartItems));
            view.setChangeText(change.signum() < 0
                    ? "Short by " + MoneyFormatUtil.format(change.abs()) : MoneyFormatUtil.format(change));
        } catch (IllegalArgumentException e) {
            view.setChangeText("Enter a valid amount");
        }
    }

    public void finalizeSale(String receivedText) {

        if (loadingState == LoadingState.LOADING) return;

        if (cartItems.isEmpty()) {
            view.showError("Cart is empty");
            return;
        }

        final BigDecimal amountReceived;
        try {
            amountReceived = SaleService.parseAmountReceived(receivedText);
            SaleService.validatePayment(saleService.calculateTotal(cartItems), amountReceived);
        } catch (IllegalArgumentException e) {
            view.showError(e.getMessage());
            return;
        }

        loadingState =
                LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus("Finalizing order...");

        List<SaleCartItem> saleItems =
                new ArrayList<>(cartItems);

        AsyncTaskRunner.run(
                () -> saleService.finalizeSale(saleItems, amountReceived),
                order -> {
                    cartItems.clear();
                    view.clearEntryForm();
                    view.resetPayment();
                    updateCartView();
                    view.showOrderCompleted(order);
                    loadingState = LoadingState.SUCCESS;
                    loadSaleData();
                },
                throwable -> {
                    loadingState =
                            LoadingState.ERROR;
                    view.setStatus("Cannot finalize order");
                    view.showError(throwable.getMessage());
                    view.setBusy(false);
                },
                null
        );
    }

    public void onSelectedProductChanged(
            Product product
    ) {

        SelectedProductPreviewDto preview =
                saleService.buildSelectedProductPreview(
                        product,
                        stockByProductId
                );

        if (preview == null) {
            view.clearSelectedProductPreview();
            return;
        }

        view.showSelectedProductPreview(preview);
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
        updatePayment(view.getAmountReceivedText());
    }

    private record SaleData(
            List<Product> products,
            List<Product> quickPickProducts,
            List<SaleOrder> orders,
            Map<Long, Integer> stockByProductId
    ) {
    }
}
