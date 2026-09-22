package com.customershopfx.product.viewmodel;

import com.customershopfx.product.model.Category;
import com.customershopfx.product.model.Product;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Collection;

public class ProductViewModel {
    private final ObservableList<Product> products = FXCollections.observableArrayList();
    private final ObservableList<Category> categories = FXCollections.observableArrayList();
    private final ObjectProperty<Category> selectedCategory = new SimpleObjectProperty<>();
    private final StringProperty keyword = new SimpleStringProperty("");
    private final StringProperty selectedSort = new SimpleStringProperty("name_asc");
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final StringProperty errorMessage = new SimpleStringProperty("");

    public ObservableList<Product> getProducts() {
        return products;
    }

    public ObservableList<Category> getCategories() {
        return categories;
    }

    public void setProducts(Collection<? extends Product> items) {
        products.setAll(items);
    }

    public void setCategories(Collection<? extends Category> items) {
        categories.setAll(items);
    }

    public Category getSelectedCategory() {
        return selectedCategory.get();
    }

    public void setSelectedCategory(Category selectedCategory) {
        this.selectedCategory.set(selectedCategory);
    }

    public ObjectProperty<Category> selectedCategoryProperty() {
        return selectedCategory;
    }

    public String getKeyword() {
        return keyword.get();
    }

    public void setKeyword(String keyword) {
        this.keyword.set(keyword == null ? "" : keyword);
    }

    public StringProperty keywordProperty() {
        return keyword;
    }

    public String getSelectedSort() {
        return selectedSort.get();
    }

    public void setSelectedSort(String selectedSort) {
        this.selectedSort.set(selectedSort == null ? "" : selectedSort);
    }

    public StringProperty selectedSortProperty() {
        return selectedSort;
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
