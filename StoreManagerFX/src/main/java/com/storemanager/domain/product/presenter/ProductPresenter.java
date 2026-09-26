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

public class ProductPresenter extends BaseCrudPresenter<Product> {

    private final ProductListController view;

    private final ProductService productService =
            new ProductService();

    private Map<Long, Category> categoriesById =
            Map.of();

    private LoadingState loadingState =
            LoadingState.IDLE;

    public ProductPresenter(
            ProductListController view
    ) {

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

        loadingState =
                LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus("Loading products...");

        AsyncTaskRunner.run(
                () -> new ProductData(
                        productService.findAll(),
                        productService.findCategories(),
                        productService.findCategoriesById()
                ),
                data -> {
                    categoriesById =
                            data.categoriesById();
                    view.setCategories(data.categories());
                    view.setProducts(data.products());
                    loadingState =
                            LoadingState.SUCCESS;
                    view.setStatus("Ready");
                    updateActionState();
                },
                throwable -> {
                    loadingState =
                            LoadingState.ERROR;
                    view.setStatus("Unable to load products");
                    view.showError(throwable.getMessage());
                },
                () -> view.setBusy(false)
        );
    }

    public void selectProduct(
            Product product
    ) {

        if (product == null) {
            enterCreateMode();
            view.clearProductForm();
            updateActionState();
            return;
        }

        enterEditMode(product);
        view.showProduct(product);
        updateActionState();
    }

    public void saveProduct(
            Product formProduct
    ) {

        try {

            boolean success;

            if (getMode() == CrudMode.CREATE) {
                success =
                        productService.create(formProduct);
            } else {
                formProduct.setId(
                        getSelectedEntity().getId()
                );
                success =
                        productService.update(formProduct);
            }

            if (!success) {
                view.showError(
                        getMode() == CrudMode.CREATE
                                ? "Cannot create product"
                                : "Cannot update product"
                );
                return;
            }

            clearForm();
            refreshTable();

        } catch (Exception e) {

            view.showError(e.getMessage());
        }
    }

    public void deleteProduct() {

        if (!hasSelection()) {
            view.showError("Select a product to deactivate");
            return;
        }

        try {

            boolean success =
                    productService.delete(
                            getSelectedEntity()
                    );

            if (!success) {
                view.showError("Unable to deactivate product");
                return;
            }

            clearForm();
            refreshTable();

        } catch (Exception e) {

            view.showError(e.getMessage());
        }
    }

    public void clearForm() {

        enterCreateMode();
        view.clearSelection();
        view.clearProductForm();
        updateActionState();
    }

    public String getCategoryName(
            Product product
    ) {

        if (product == null || product.getCategoryId() == null) {
            return "";
        }

        Category category =
                categoriesById.get(
                        product.getCategoryId()
                );

        if (category == null) {
            return "";
        }

        return category.getName();
    }

    public LoadingState getLoadingState() {
        return loadingState;
    }

    private void updateActionState() {

        boolean hasSelection =
                hasSelection();

        view.setUpdateEnabled(hasSelection);
        view.setDeleteEnabled(hasSelection);
    }

    private record ProductData(
            List<Product> products,
            List<Category> categories,
            Map<Long, Category> categoriesById
    ) {
    }
}
