package com.storemanager.core.runtime;

public class ModuleState<T> {

    private CrudMode mode =
            CrudMode.CREATE;

    private T selectedEntity;

    public CrudMode getMode() {
        return mode;
    }

    public void setMode(
            CrudMode mode
    ) {
        this.mode = mode;
    }

    public T getSelectedEntity() {
        return selectedEntity;
    }

    public void setSelectedEntity(
            T selectedEntity
    ) {
        this.selectedEntity = selectedEntity;
    }

    public boolean hasSelection() {

        return selectedEntity != null;
    }

    public void clearSelection() {

        selectedEntity = null;
    }
}
