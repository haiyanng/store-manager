package com.storemanager.domain.category.presenter;

import com.storemanager.core.runtime.BaseCrudPresenter;
import com.storemanager.core.runtime.CrudMode;
import com.storemanager.core.runtime.async.AsyncTaskRunner;
import com.storemanager.core.runtime.async.LoadingState;
import com.storemanager.domain.category.model.Category;
import com.storemanager.domain.category.service.CategoryService;
import com.storemanager.domain.category.view.CategoryListController;

public class CategoryPresenter extends BaseCrudPresenter<Category> {

    private final CategoryListController view;

    private final CategoryService categoryService =
            new CategoryService();

    private LoadingState loadingState =
            LoadingState.IDLE;

    public CategoryPresenter(
            CategoryListController view
    ) {

        this.view = view;
    }

    @Override
    public void initialize() {

        enterCreateMode();
        loadCategories();
    }

    public void loadCategories() {

        refreshTable();
    }

    public void refreshTable() {

        loadingState =
                LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus("Loading categories...");

        AsyncTaskRunner.run(
                categoryService::findAll,
                categories -> {
                    view.setCategories(categories);
                    loadingState =
                            LoadingState.SUCCESS;
                    view.setStatus("Ready");
                    updateActionState();
                },
                throwable -> {
                    loadingState =
                            LoadingState.ERROR;
                    view.setStatus("Unable to load categories");
                    view.showError(throwable.getMessage());
                },
                () -> view.setBusy(false)
        );
    }

    public void selectCategory(
            Category category
    ) {

        if (category == null) {
            enterCreateMode();
            view.clearCategoryForm();
            updateActionState();
            return;
        }

        enterEditMode(category);
        view.showCategory(category);
        updateActionState();
    }

    public void saveCategory(
            Category formCategory
    ) {

        try {

            boolean success;

            if (getMode() == CrudMode.CREATE) {
                success =
                        categoryService.create(formCategory);
            } else {
                formCategory.setId(
                        getSelectedEntity().getId()
                );
                success =
                        categoryService.update(formCategory);
            }

            if (!success) {
                view.showError(
                        getMode() == CrudMode.CREATE
                                ? "Cannot create category"
                                : "Cannot update category"
                );
                return;
            }

            clearForm();
            refreshTable();

        } catch (Exception e) {

            view.showError(e.getMessage());
        }
    }

    public void deactivateCategory() {

        if (!hasSelection()) {
            view.showError("Select a category to deactivate");
            return;
        }

        try {

            boolean success =
                    categoryService.deactivate(
                            getSelectedEntity()
                    );

            if (!success) {
                view.showError("Cannot deactivate category");
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
        view.clearCategoryForm();
        updateActionState();
    }

    public LoadingState getLoadingState() {
        return loadingState;
    }

    private void updateActionState() {

        boolean hasSelection =
                hasSelection();

        view.setUpdateEnabled(hasSelection);
        view.setDeactivateEnabled(hasSelection);
    }
}
