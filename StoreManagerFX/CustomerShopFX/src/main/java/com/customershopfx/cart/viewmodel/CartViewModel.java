package com.customershopfx.cart.viewmodel;

import com.customershopfx.cart.model.Cart;
import com.customershopfx.cart.model.CartItem;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.math.BigDecimal;
import java.util.Collection;

public class CartViewModel {
    private final ObjectProperty<Cart> cart = new SimpleObjectProperty<>();
    private final ObservableList<CartItem> items = FXCollections.observableArrayList();
    private final ObjectProperty<BigDecimal> total = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final StringProperty errorMessage = new SimpleStringProperty("");
    private final IntegerProperty itemCount = new SimpleIntegerProperty(0);

    public Cart getCart() {
        return cart.get();
    }

    public void setCart(Cart cart) {
        this.cart.set(cart);
        if (cart == null) {
            items.clear();
            total.set(BigDecimal.ZERO);
            itemCount.set(0);
            return;
        }
        setItems(cart.items());
        total.set(cart.total() == null ? BigDecimal.ZERO : cart.total());
        itemCount.set(items.stream().mapToInt(CartItem::quantity).sum());
    }

    public ObjectProperty<Cart> cartProperty() {
        return cart;
    }

    public ObservableList<CartItem> getItems() {
        return items;
    }

    public void setItems(Collection<? extends CartItem> items) {
        this.items.setAll(items);
        itemCount.set(this.items.stream().mapToInt(CartItem::quantity).sum());
    }

    public BigDecimal getTotal() {
        BigDecimal value = total.get();
        return value == null ? BigDecimal.ZERO : value;
    }

    public void setTotal(BigDecimal total) {
        this.total.set(total == null ? BigDecimal.ZERO : total);
    }

    public ObjectProperty<BigDecimal> totalProperty() {
        return total;
    }

    public int getItemCount() {
        return itemCount.get();
    }

    public void setItemCount(int itemCount) {
        this.itemCount.set(itemCount);
    }

    public IntegerProperty itemCountProperty() {
        return itemCount;
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

    public void setErrorMessage(String errorMessage) {
        this.errorMessage.set(errorMessage == null ? "" : errorMessage);
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
