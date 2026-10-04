package com.storemanager.domain.inventory.presenter;

import com.storemanager.core.runtime.BaseModulePresenter;
import com.storemanager.core.runtime.async.AsyncTaskRunner;
import com.storemanager.core.runtime.async.LoadingState;
import com.storemanager.domain.inventory.model.InventoryItem;
import com.storemanager.domain.inventory.model.InventoryTransaction;
import com.storemanager.domain.inventory.service.InventoryService;
import com.storemanager.domain.inventory.view.InventoryListController;
import com.storemanager.domain.product.model.Product;

import java.util.List;
import java.util.Map;

public class InventoryPresenter extends BaseModulePresenter {

    private final InventoryListController view;

    private final InventoryService inventoryService =
            new InventoryService();

    private Map<Long, Product> productsById =
            Map.of();

    private List<InventoryItem> items = List.of();
    private List<InventoryTransaction> transactions = List.of();

    private LoadingState loadingState =
            LoadingState.IDLE;

    public InventoryPresenter(
            InventoryListController view
    ) {

        this.view = view;
    }

    @Override
    public void initialize() {

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
                        inventoryService.findItemsWithExpiry(),
                        inventoryService.findAllTransactions(),
                        inventoryService.findProductsById()
                ),
                data -> {
                    productsById =
                            data.productsById();
                    items = data.items();
                    transactions = data.transactions();
                    view.setFilterProducts(productsById.values().stream()
                            .sorted(java.util.Comparator.comparing(Product::getName)).toList());
                    applyFilters();
                    loadingState =
                            LoadingState.SUCCESS;
                    view.setStatus("Ready");
                },
                throwable -> {
                    loadingState =
                            LoadingState.ERROR;
                    view.setStatus("Unable to load inventory");
                    view.showError(throwable.getMessage());
                },
                () -> view.setBusy(false)
        );
    }

    public void applyFilters() {
        try {
            view.setInventoryItems(items.stream()
                    .filter(item -> !view.isLowStockOnly() || InventoryService.isLowStock(item)).toList());
            view.setTransactions(InventoryService.filterImportHistory(transactions,
                    view.getFromDate(), view.getToDate(), view.getFilterProductId()));
        } catch (IllegalArgumentException e) { view.showError(e.getMessage()); }
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
            Map<Long, Product> productsById
    ) {
    }
}
