package com.customershopfx.order.viewmodel;

import com.customershopfx.order.model.Order;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Collection;

public class OrderViewModel {
    private final ObservableList<Order> orders = FXCollections.observableArrayList();
    private final ObjectProperty<Order> selectedOrder = new SimpleObjectProperty<>();
    private final StringProperty checkoutName = new SimpleStringProperty("");
    private final StringProperty checkoutPhone = new SimpleStringProperty("");
    private final StringProperty checkoutAddress = new SimpleStringProperty("");
    private final StringProperty paymentMethod = new SimpleStringProperty("Cash on delivery");
    private final StringProperty checkoutMessage = new SimpleStringProperty("");
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final StringProperty errorMessage = new SimpleStringProperty("");

    public ObservableList<Order> getOrders() {
        return orders;
    }

    public void setOrders(Collection<? extends Order> items) {
        orders.setAll(items);
    }

    public Order getSelectedOrder() {
        return selectedOrder.get();
    }

    public void setSelectedOrder(Order selectedOrder) {
        this.selectedOrder.set(selectedOrder);
    }

    public ObjectProperty<Order> selectedOrderProperty() {
        return selectedOrder;
    }

    public String getCheckoutName() {
        return checkoutName.get();
    }

    public void setCheckoutName(String value) {
        checkoutName.set(value == null ? "" : value);
    }

    public StringProperty checkoutNameProperty() {
        return checkoutName;
    }

    public String getCheckoutPhone() {
        return checkoutPhone.get();
    }

    public void setCheckoutPhone(String value) {
        checkoutPhone.set(value == null ? "" : value);
    }

    public StringProperty checkoutPhoneProperty() {
        return checkoutPhone;
    }

    public String getCheckoutAddress() {
        return checkoutAddress.get();
    }

    public void setCheckoutAddress(String value) {
        checkoutAddress.set(value == null ? "" : value);
    }

    public StringProperty checkoutAddressProperty() {
        return checkoutAddress;
    }

    public String getPaymentMethod() {
        return paymentMethod.get();
    }

    public void setPaymentMethod(String value) {
        paymentMethod.set(value == null || value.isBlank() ? "Cash on delivery" : value);
    }

    public StringProperty paymentMethodProperty() {
        return paymentMethod;
    }

    public String getCheckoutMessage() {
        return checkoutMessage.get();
    }

    public void setCheckoutMessage(String value) {
        checkoutMessage.set(value == null ? "" : value);
    }

    public StringProperty checkoutMessageProperty() {
        return checkoutMessage;
    }

    public boolean isLoading() {
        return loading.get();
    }

    public void setLoading(boolean loading) {
        this.loading.set(loading);
    }

    public BooleanProperty loadingProperty() {
        return loading;
    }

    public String getErrorMessage() {
        return errorMessage.get();
    }

    public void setErrorMessage(String value) {
        errorMessage.set(value == null ? "" : value);
    }

    public StringProperty errorMessageProperty() {
        return errorMessage;
    }

    public void markLoading() {
        setLoading(true);
        setErrorMessage("");
    }

    public void markFailure(String message) {
        setLoading(false);
        setErrorMessage(message);
    }

    public void markReady() {
        setLoading(false);
        setErrorMessage("");
    }
}
