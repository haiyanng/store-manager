package com.storemanager.domain.inventory.presenter;

import com.storemanager.core.runtime.BaseCrudPresenter;
import com.storemanager.core.runtime.async.AsyncTaskRunner;
import com.storemanager.core.runtime.async.LoadingState;
import com.storemanager.domain.inventory.model.InventoryItem;
import com.storemanager.domain.inventory.model.InventoryTransaction;
import com.storemanager.domain.inventory.service.InventoryService;
import com.storemanager.domain.inventory.view.InventoryListController;
import com.storemanager.domain.product.model.Product;

import java.util.List;
import java.util.Map;

public class InventoryPresenter extends BaseCrudPresenter<InventoryItem> {

    private final InventoryListController view;

    private final InventoryService inventoryService =
            new InventoryService();

    private Map<Long, Product> productsById =
            Map.of();

    private LoadingState loadingState =
            LoadingState.IDLE;

    public InventoryPresenter(
            InventoryListController view
    ) {

        this.view = view;
    }

    @Override
    public void initialize() {

        enterCreateMode();
        loadInventory();
    }

    public void loadInventory() {

        refresh();
    }

    public void refresh() {

        loadingState =
                LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus("Loading inventory...");

        AsyncTaskRunner.run(
                () -> new InventoryData(
                        inventoryService.findAllItems(),
                        inventoryService.findAllTransactions(),
                        inventoryService.findProducts(),
                        inventoryService.findProductsById()
                ),
                data -> {
                    productsById =
                            data.productsById();
                    view.setProducts(data.products());
                    view.setInventoryItems(data.items());
                    view.setTransactions(data.transactions());
                    loadingState =
                            LoadingState.SUCCESS;
                    view.setStatus("Ready");
                },
                throwable -> {
                    loadingState =
                            LoadingState.ERROR;
                    view.setStatus("Cannot load inventory");
                    view.showError(throwable.getMessage());
                },
                () -> view.setBusy(false)
        );
    }

    public void adjustStock(
            InventoryTransaction transaction
    ) {

        loadingState =
                LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus("Applying stock transaction...");

        AsyncTaskRunner.run(
                () -> inventoryService.adjustStock(transaction),
                success -> {
                    if (!success) {
                        view.showError("Cannot apply stock transaction");
                        view.setStatus("Cannot apply stock transaction");
                        view.setBusy(false);
                        return;
                    }

                    view.clearAdjustmentForm();
                    refresh();
                },
                throwable -> {
                    loadingState =
                            LoadingState.ERROR;
                    view.setStatus("Cannot apply stock transaction");
                    view.showError(throwable.getMessage());
                    view.setBusy(false);
                },
                null
        );
    }

    public void clearForm() {

        enterCreateMode();
        view.clearAdjustmentForm();
    }

    public String getProductName(
            Long productId
    ) {

        if (productId == null) {
            return "";
        }

        Product product =
                productsById.get(productId);

        if (product == null) {
            return "";
        }

        return product.getName();
    }

    public String getProductSku(
            Long productId
    ) {

        if (productId == null) {
            return "";
        }

        Product product =
                productsById.get(productId);

        if (product == null) {
            return "";
        }

        return product.getSku();
    }

    public LoadingState getLoadingState() {
        return loadingState;
    }

    private record InventoryData(
            List<InventoryItem> items,
            List<InventoryTransaction> transactions,
            List<Product> products,
            Map<Long, Product> productsById
    ) {
    }
}
