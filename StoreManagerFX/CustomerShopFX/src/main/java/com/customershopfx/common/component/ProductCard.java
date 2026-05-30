package com.customershopfx.common.component;

import atlantafx.base.controls.Card;
import atlantafx.base.theme.Styles;
import com.customershopfx.product.model.Product;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ProductCard extends Card {
    private final Product product;
    private final Consumer<Product> onSelect;
    private final BiConsumer<Product, Integer> onAddToCart;
    private final Spinner<Integer> quantitySpinner = new Spinner<>();

    public ProductCard(Product product,
                       NumberFormat money,
                       boolean selected,
                       Consumer<Product> onSelect,
                       BiConsumer<Product, Integer> onAddToCart) {
        this.product = Objects.requireNonNull(product, "product");
        this.onSelect = onSelect;
        this.onAddToCart = onAddToCart;

        getStyleClass().addAll("product-card", "product-card-clickable");
        if (selected) {
            getStyleClass().add("product-card-selected");
        }
        setMinWidth(224);
        setPrefWidth(224);
        setMaxWidth(224);
        setCursor(Cursor.HAND);

        StackPane image = productImage(product.name(), product.imagePath(), 200, 150);
        Label name = new Label(product.name());
        name.getStyleClass().addAll("text-card-title", "product-title");
        name.setWrapText(true);

        Label meta = new Label((product.categoryName() == null ? "General" : product.categoryName()) + " | " + product.unit());
        meta.getStyleClass().addAll("text-meta", "product-meta");
        meta.setWrapText(true);

        Label price = new Label(formatMoney(money, product.basePrice()));
        price.getStyleClass().addAll("text-price", "product-price");

        Label shipping = new Label("Fast local delivery");
        shipping.getStyleClass().addAll("text-meta", "product-meta");

        quantitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99, 1));
        quantitySpinner.setMaxWidth(86);
        quantitySpinner.getStyleClass().addAll("large", "rounded");

        Button add = new Button("Add to Cart");
        add.setMaxWidth(Double.MAX_VALUE);
        add.getStyleClass().addAll("button", Styles.ACCENT, Styles.ROUNDED, Styles.LARGE);
        add.setOnAction(event -> {
            if (onAddToCart != null) {
                onAddToCart.accept(product, quantitySpinner.getValue());
            }
        });
        add.setOnMouseClicked(event -> event.consume());

        HBox priceRow = new HBox(10, price, spacer(), quantitySpinner);
        HBox actionRow = new HBox(add);
        actionRow.getStyleClass().add("product-action-row");
        HBox.setHgrow(add, Priority.ALWAYS);

        VBox body = new VBox(12, name, meta, priceRow, shipping, actionRow);
        body.setAlignment(Pos.TOP_LEFT);
        body.setPadding(new Insets(12));
        setHeader(image);
        setBody(body);

        setOnMouseClicked(event -> {
            if (onSelect != null) {
                onSelect.accept(product);
            }
        });
    }

    private String formatMoney(NumberFormat money, BigDecimal value) {
        return money == null || value == null ? "0 VND" : money.format(value);
    }

    private StackPane productImage(String name, String imagePath, double width, double height) {
        StackPane wrap = new StackPane();
        wrap.getStyleClass().addAll("product-image", "product-image-placeholder");
        wrap.setPrefSize(width, height);
        wrap.setMinSize(width, height);
        wrap.setMaxSize(width, height);

        if (imagePath != null && !imagePath.isBlank() && imagePath.startsWith("http")) {
            Image image = new Image(imagePath, width, height, true, true, true);
            ImageView view = new ImageView(image);
            view.setFitWidth(width);
            view.setFitHeight(height);
            view.setPreserveRatio(true);
            view.setSmooth(true);
            wrap.getChildren().add(view);
            return wrap;
        }

        String initial = name == null || name.isBlank() ? "P" : name.substring(0, 1).toUpperCase(Locale.ROOT);
        Label placeholder = new Label(initial);
        placeholder.getStyleClass().add("image-placeholder");
        wrap.getChildren().add(placeholder);
        return wrap;
    }

    private Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }
}
