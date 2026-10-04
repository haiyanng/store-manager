package com.storemanager.domain.category.presenter;

import com.storemanager.core.runtime.BaseCrudPresenter;
import com.storemanager.core.runtime.CrudMode;
import com.storemanager.core.runtime.async.AsyncTaskRunner;
import com.storemanager.core.runtime.async.LoadingState;
import com.storemanager.domain.category.model.Category;
import com.storemanager.domain.category.service.CategoryService;
import com.storemanager.domain.category.view.CategoryListController;

import java.util.List;
import java.util.concurrent.Callable;

public class CategoryPresenter extends BaseCrudPresenter<Category> {

    private final CategoryListController view;
    private final CategoryService categoryService = new CategoryService();
    private LoadingState loadingState = LoadingState.IDLE;
    private boolean busy;

    public CategoryPresenter(CategoryListController view) {
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
        runTask(categoryService::findAll, "Loading categories...", "Unable to load categories");
    }

    public void selectCategory(Category category) {
        if (busy) return;
        if (category == null) {
            enterCreateMode();
            view.clearCategoryForm();
        } else {
            enterEditMode(category);
            view.showCategory(category);
        }
        updateActionState();
    }

    public void saveCategory(Category formCategory) {
        if (busy) return;
        boolean creating = getMode() == CrudMode.CREATE;
        if (!creating) formCategory.setId(getSelectedEntity().getId());
        runMutation(() -> creating
                ? categoryService.create(formCategory)
                : categoryService.update(formCategory),
                "Saving category...", "Unable to save category");
    }

    public void deactivateCategory() {
        if (busy) return;
        if (!hasSelection()) {
            view.showError("Select a category to deactivate");
            return;
        }
        Category selected = getSelectedEntity();
        runMutation(() -> categoryService.deactivate(selected),
                "Deactivating category...", "Unable to deactivate category");
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
                        runTask(categoryService::findAll, "Refreshing categories...",
                                "Changes saved; unable to refresh the list. Click Refresh to reload.");
                    } else {
                        busy = false;
                        view.setBusy(false);
                        updateActionState();
                    }
                });
    }

    private void runTask(Callable<List<Category>> task, String status, String failureStatus) {
        if (busy) return;
        busy = true;
        loadingState = LoadingState.LOADING;
        view.setBusy(true);
        view.setStatus(status);
        AsyncTaskRunner.run(task,
                categories -> {
                    clearForm();
                    view.setCategories(categories);
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
        view.setDeactivateEnabled(hasSelection());
    }
}
