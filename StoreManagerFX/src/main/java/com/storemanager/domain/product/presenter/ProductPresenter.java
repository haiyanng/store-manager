package com.storemanager.domain.product.presenter;

import com.storemanager.core.runtime.BaseCrudPresenter;
import com.storemanager.core.runtime.CrudMode;
import com.storemanager.core.runtime.async.AsyncTaskRunner;
import com.storemanager.core.runtime.async.LoadingState;
import com.storemanager.domain.category.model.Category;
import com.storemanager.domain.product.model.Product;
import com.storemanager.domain.product.service.ProductService;
import com.storemanager.domain.product.view.ProductListController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ProductPresenter extends BaseCrudPresenter<Product> {

    private final ProductListController view;
    private final ProductService productService = new ProductService();
    private Map<Long, Category> categoriesById = Map.of();
    private LoadingState loadingState = LoadingState.IDLE;
    private boolean busy;

    public ProductPresenter(ProductListController view) {
        this.view = view;
    }

    @Override
    public void initialize() {
        enterCreateMode();
        loadProducts();
    }

    public void loadProducts() {
        refreshTable();
    }

    public void refreshTable() {
        runTask(this::loadData, "Loading products...", "Unable to load products");
    }

    public void selectProduct(Product product) {
        if (busy) return;
        if (product == null) {
            enterCreateMode();
            view.clearProductForm();
        } else {
            enterEditMode(product);
            view.showProduct(product);
        }
        updateActionState();
    }

    public void saveProduct(Product formProduct) {
        if (busy) return;
        boolean creating = getMode() == CrudMode.CREATE;
        if (!creating) formProduct.setId(getSelectedEntity().getId());
        runMutation(() -> creating
                ? productService.create(formProduct)
                : productService.update(formProduct),
                "Saving product...", "Unable to save product");
    }

    public void clearForm() {
        enterCreateMode();
        view.clearSelection();
        view.clearProductForm();
        updateActionState();
    }

    public String getCategoryName(Product product) {
        if (product == null || product.getCategoryId() == null) return "";
        Category category = categoriesById.get(product.getCategoryId());
        return category == null ? "" : category.getName();
    }

    public LoadingState getLoadingState() {
        return loadingState;
    }

    private ProductData loadData() {
        List<Category> categories = productService.findCategories();
        Map<Long, Category> byId = categories.stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));
        return new ProductData(productService.findAll(), categories, byId);
    }


    private void runMutation(Callable<Boolean> mutation, String status, String failureStatus) {
        if (busy) return;
        busy = true;
        loadingState = LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus(status);
        boolean[] saved = {false};
        AsyncTaskRunner.run(mutation,
                success -> {
                    if (!success) {
                        loadingState = LoadingState.ERROR;
                        view.setStatus(failureStatus);
                        view.showError(failureStatus);
                        return;
                    }
                    saved[0] = true;
                    clearForm();
                }, error -> {
                    loadingState = LoadingState.ERROR;
                    view.setStatus(failureStatus);
                    view.showError(error.getMessage());
                }, () -> {
                    if (saved[0]) {
                        busy = false;
                        runTask(this::loadData, "Refreshing products...",
                                "Changes saved; unable to refresh the list. Click Refresh to reload.");
                    } else {
                        busy = false;
                        view.setBusy(false);
                        updateActionState();
                    }
                });
    }

    private void runTask(Callable<ProductData> task, String status, String failureStatus) {
        if (busy) return;
        busy = true;
        loadingState = LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus(status);
        AsyncTaskRunner.run(task,
                data -> {
                    clearForm();
                    categoriesById = data.categoriesById();
                    view.setCategories(data.categories());
                    view.setProducts(data.products());
                    loadingState = LoadingState.SUCCESS;
                    view.setStatus("Ready");
                }, error -> {
                    loadingState = LoadingState.ERROR;
                    view.setStatus(failureStatus);
                    view.showError(error.getMessage());
                }, () -> {
                    busy = false;
                    view.setBusy(false);
                    updateActionState();
                });
    }

    private void updateActionState() {
        view.setUpdateEnabled(hasSelection());
    }

    private record ProductData(List<Product> products, List<Category> categories, Map<Long, Category> categoriesById) {
    }
}
