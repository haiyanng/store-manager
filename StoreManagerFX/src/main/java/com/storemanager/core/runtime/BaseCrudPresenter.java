package com.storemanager.core.runtime;

public abstract class BaseCrudPresenter<T> extends BaseModulePresenter {

    private final ModuleState<T> state =
            new ModuleState<>();

    protected ModuleState<T> getState() {
        return state;
    }

    protected CrudMode getMode() {
        return state.getMode();
    }

    protected void setMode(
            CrudMode mode
    ) {
        state.setMode(mode);
    }

    protected T getSelectedEntity() {
        return state.getSelectedEntity();
    }

    protected void setSelectedEntity(
            T selectedEntity
    ) {
        state.setSelectedEntity(selectedEntity);
    }

    protected boolean hasSelection() {
        return state.hasSelection();
    }

    protected void clearSelectedEntity() {
        state.clearSelection();
    }

    protected void enterCreateMode() {

        clearSelectedEntity();
        setMode(
                CrudMode.CREATE
        );
    }

    protected void enterEditMode(
            T selectedEntity
    ) {

        setSelectedEntity(selectedEntity);
        setMode(
                CrudMode.EDIT
        );
    }

    protected void enterViewMode(
            T selectedEntity
    ) {

        setSelectedEntity(selectedEntity);
        setMode(
                CrudMode.VIEW
        );
    }
}
