package com.storemanager.domain.importing.presenter;

import com.storemanager.core.runtime.BaseModulePresenter;
import com.storemanager.core.runtime.async.AsyncTaskRunner;
import com.storemanager.core.runtime.async.LoadingState;
import com.storemanager.domain.importing.model.ImportCartItem;
import com.storemanager.domain.importing.model.ImportReceipt;
import com.storemanager.domain.importing.service.ImportService;
import com.storemanager.domain.importing.view.ImportController;
import com.storemanager.domain.product.model.Product;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.ArrayList;
import java.util.List;

public class ImportPresenter extends BaseModulePresenter {

    private final ImportController view;

    private final ImportService importService =
            new ImportService();

    private final List<ImportCartItem> importItems =
            new ArrayList<>();

    private LoadingState loadingState =
            LoadingState.IDLE;

    public ImportPresenter(
            ImportController view
    ) {

        this.view = view;
    }

    @Override
    public void initialize() {

        loadImportData();
        updateItemView();
    }

    public void loadImportData() {

        if (loadingState == LoadingState.LOADING) return;

        loadingState =
                LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus("Loading import workspace...");

        AsyncTaskRunner.run(
                () -> new ImportData(
                        importService.findProducts(),
                        importService.findQuickPickProducts(20),
                        importService.findRecentReceipts()
                ),
                data -> {
                    view.setProducts(data.products());
                    view.setQuickPickProducts(data.quickPickProducts());
                    view.setRecentReceipts(data.receipts());
                    loadingState =
                            LoadingState.SUCCESS;
                    view.setStatus("Ready");
                },
                throwable -> {
                    loadingState =
                            LoadingState.ERROR;
                    view.setStatus("Unable to load import workspace");
                    view.showError(throwable.getMessage());
                },
                () -> view.setBusy(false)
        );
    }

    public void addItem(
            Product product,
            int quantity,
            BigDecimal unitCost,
            LocalDate expiryDate
    ) {

        if (loadingState == LoadingState.LOADING) return;

        try {

            ImportService.validateExpiryDate(expiryDate);

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

            if (unitCost == null
                    || unitCost.compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException(
                        "Unit cost cannot be negative"
                );
            }

            if (unitCost.stripTrailingZeros().scale() > 2) {
                throw new IllegalArgumentException("Unit cost must have at most 2 decimal places");
            }

            ImportCartItem existingItem =
                    findImportItem(product, unitCost, expiryDate);

            if (existingItem == null) {
                ImportCartItem item = new ImportCartItem(product, quantity, unitCost);
                item.setExpiryDate(expiryDate);
                importItems.add(item);
            } else {
                existingItem.setQuantity(
                        Math.addExact(existingItem.getQuantity(), quantity)
                );
            }

            view.clearEntryForm();
            updateItemView();

        } catch (Exception e) {

            view.showError(e.getMessage());
        }
    }

    public void removeItem(
            ImportCartItem item
    ) {

        if (loadingState == LoadingState.LOADING) return;

        if (item == null) {
            view.showError("Select an import item to remove");
            return;
        }

        importItems.remove(item);
        updateItemView();
    }

    public void clearItems() {

        if (loadingState == LoadingState.LOADING) return;

        importItems.clear();
        view.clearEntryForm();
        updateItemView();
    }

    public void finalizeImport(
            String supplierName
    ) {

        if (loadingState == LoadingState.LOADING) return;

        if (importItems.isEmpty()) {
            view.showError("Import item list is empty");
            return;
        }

        loadingState =
                LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus("Finalizing import...");

        List<ImportCartItem> items =
                new ArrayList<>(importItems);

        AsyncTaskRunner.run(
                () -> importService.finalizeImport(
                        supplierName,
                        items
                ),
                receiptId -> {
                    importItems.clear();
                    view.clearEntryForm();
                    view.clearSupplier();
                    updateItemView();
                    view.setStatus("Import receipt #" + receiptId + " created");
                    loadingState = LoadingState.SUCCESS;
                    loadImportData();
                },
                throwable -> {
                    loadingState =
                            LoadingState.ERROR;
                    view.setStatus("Cannot finalize import");
                    view.showError(throwable.getMessage());
                    view.setBusy(false);
                },
                null
        );
    }

    public LoadingState getLoadingState() {
        return loadingState;
    }

    private ImportCartItem findImportItem(
            Product product,
            BigDecimal unitCost,
            LocalDate expiryDate
    ) {

        return importItems
                .stream()
                .filter(item ->
                        item.getProduct() != null
                                && product.getId().equals(
                                item.getProduct().getId()
                        )
                                && item.getUnitCost().compareTo(unitCost) == 0
                                && Objects.equals(item.getExpiryDate(), expiryDate)
                )
                .findFirst()
                .orElse(null);
    }

    private void updateItemView() {

        view.setImportItems(
                new ArrayList<>(importItems)
        );

        view.setTotalCost(
                importService.calculateTotal(importItems)
        );
    }

    private record ImportData(
            List<Product> products,
            List<Product> quickPickProducts,
            List<ImportReceipt> receipts
    ) {
    }
}
