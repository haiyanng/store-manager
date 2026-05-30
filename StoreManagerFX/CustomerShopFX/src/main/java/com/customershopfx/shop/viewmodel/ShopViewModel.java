package com.customershopfx.shop.viewmodel;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ShopViewModel {
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final IntegerProperty selectedTab = new SimpleIntegerProperty(0);
    private final StringProperty errorMessage = new SimpleStringProperty("");
    private final StringProperty statusMessage = new SimpleStringProperty("Ready");

    public boolean isLoading() {
        return loading.get();
    }

    public void setLoading(boolean loading) {
        this.loading.set(loading);
    }

    public BooleanProperty loadingProperty() {
        return loading;
    }

    public int getSelectedTab() {
        return selectedTab.get();
    }

    public void setSelectedTab(int selectedTab) {
        this.selectedTab.set(selectedTab);
    }

    public IntegerProperty selectedTabProperty() {
        return selectedTab;
    }

    public String getErrorMessage() {
        return errorMessage.get();
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage.set(errorMessage == null ? "" : errorMessage);
    }

    public StringProperty errorMessageProperty() {
        return errorMessage;
    }

    public String getStatusMessage() {
        return statusMessage.get();
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage.set(statusMessage == null ? "" : statusMessage);
    }

    public StringProperty statusMessageProperty() {
        return statusMessage;
    }

    public void markLoading(String message) {
        setLoading(true);
        setErrorMessage("");
        setStatusMessage(message);
    }

    public void markFailure(String message) {
        setLoading(false);
        setErrorMessage(message);
        setStatusMessage(message);
    }
}
