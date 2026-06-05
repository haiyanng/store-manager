package com.customershopfx.common.component;

import atlantafx.base.controls.Card;
import atlantafx.base.theme.Styles;
import com.customershopfx.product.model.Product;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.OverrunStyle;
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
    private static final double MIN_TILE_WIDTH = 240;
    private static final double MAX_TILE_WIDTH = 320;
    private static final double TILE_HEIGHT = 388;
    private static final double CARD_PADDING = 24;
    private static final double BODY_HEIGHT = TILE_HEIGHT - CARD_PADDING;
    private static final double IMAGE_HEIGHT = 150;
    private static final double CONTENT_HEIGHT = 104;
    private static final double ACTION_HEIGHT = 72;

    private final Product product;
    private final Consumer<Product> onSelect;
    private final BiConsumer<Product, Integer> onAddToCart;
    private final Spinner<Integer> quantitySpinner = new Spinner<>();
    private final StackPane imageContainer;
    private final VBox content;
    private final VBox actionArea;
    private final VBox layout;

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
        setCursor(Cursor.HAND);

        imageContainer = productImage(product.name(), product.imagePath(), MAX_TILE_WIDTH - CARD_PADDING, IMAGE_HEIGHT);

        Label name = new Label(product.name());
        name.getStyleClass().addAll("text-card-title", "product-title");
        name.setWrapText(true);
        name.setTextOverrun(OverrunStyle.ELLIPSIS);
        name.setMinHeight(34);
        name.setPrefHeight(34);
        name.setMaxHeight(34);
        name.setMaxWidth(Double.MAX_VALUE);

        Label price = new Label(formatMoney(money, product.basePrice()));
        price.getStyleClass().addAll("text-price", "product-price");

        Label shortDescription = new Label(value(product.shortDescription()));
        shortDescription.getStyleClass().add("product-short-description");
        shortDescription.setWrapText(true);
        shortDescription.setTextOverrun(OverrunStyle.ELLIPSIS);
        shortDescription.setMinHeight(32);
        shortDescription.setPrefHeight(32);
        shortDescription.setMaxHeight(32);
        shortDescription.setMaxWidth(Double.MAX_VALUE);

        quantitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99, 1));
        quantitySpinner.setMaxWidth(96);
        quantitySpinner.setMinWidth(96);
        quantitySpinner.setPrefWidth(96);
        quantitySpinner.getStyleClass().addAll("large", "rounded");
        quantitySpinner.setOnMouseClicked(event -> event.consume());

        Button add = new Button("Add to Cart");
        add.setMaxWidth(Double.MAX_VALUE);
        add.setMinWidth(0);
        add.getStyleClass().addAll("button", Styles.ACCENT, Styles.ROUNDED, Styles.LARGE);
        add.getStyleClass().add("product-add-button");
        add.setOnAction(event -> {
            if (onAddToCart != null) {
                onAddToCart.accept(product, quantitySpinner.getValue());
            }
        });
        add.setOnMouseClicked(event -> event.consume());

        Label quantityLabel = new Label("Quantity");
        quantityLabel.getStyleClass().add("product-quantity-label");

        Region quantitySpacer = new Region();
        HBox.setHgrow(quantitySpacer, Priority.ALWAYS);

        HBox quantityRow = new HBox(12, quantityLabel, quantitySpacer, quantitySpinner);
        quantityRow.setAlignment(Pos.CENTER_LEFT);
        quantityRow.getStyleClass().add("product-quantity-row");
        quantityRow.setOnMouseClicked(event -> event.consume());

        Region verticalSpacer = new Region();
        VBox.setVgrow(verticalSpacer, Priority.ALWAYS);

        content = new VBox(4, name, shortDescription, price);
        content.getStyleClass().add("product-card-content");
        content.setAlignment(Pos.TOP_LEFT);
        content.setFillWidth(true);

        actionArea = new VBox(6, quantityRow, add);
        actionArea.getStyleClass().add("product-action-area");
        actionArea.setAlignment(Pos.BOTTOM_LEFT);
        actionArea.setFillWidth(true);

        layout = new VBox(8, imageContainer, content, verticalSpacer, actionArea);
        layout.getStyleClass().add("product-card-layout");
        layout.setAlignment(Pos.TOP_LEFT);
        layout.setFillWidth(true);

        setHeader(null);
        setSubHeader(null);
        setFooter(null);
        setBody(layout);
        setTileWidth(280);

        setOnMouseClicked(event -> {
            if (onSelect != null) {
                onSelect.accept(product);
            }
        });
    }

    public void setTileWidth(double width) {
        double tileWidth =
                Math.max(
                        MIN_TILE_WIDTH,
                        Math.min(MAX_TILE_WIDTH, width)
                );
        double bodyWidth =
                tileWidth - CARD_PADDING;

        setMinSize(tileWidth, TILE_HEIGHT);
        setPrefSize(tileWidth, TILE_HEIGHT);
        setMaxSize(tileWidth, TILE_HEIGHT);

        layout.setMinSize(bodyWidth, BODY_HEIGHT);
        layout.setPrefSize(bodyWidth, BODY_HEIGHT);
        layout.setMaxSize(bodyWidth, BODY_HEIGHT);

        content.setMinSize(bodyWidth, CONTENT_HEIGHT);
        content.setPrefSize(bodyWidth, CONTENT_HEIGHT);
        content.setMaxSize(bodyWidth, CONTENT_HEIGHT);

        actionArea.setMinSize(bodyWidth, ACTION_HEIGHT);
        actionArea.setPrefSize(bodyWidth, ACTION_HEIGHT);
        actionArea.setMaxSize(bodyWidth, ACTION_HEIGHT);

        imageContainer.setMinSize(bodyWidth, IMAGE_HEIGHT);
        imageContainer.setPrefSize(bodyWidth, IMAGE_HEIGHT);
        imageContainer.setMaxSize(bodyWidth, IMAGE_HEIGHT);

        for (Node node : imageContainer.getChildren()) {
            if (node instanceof ImageView imageView) {
                imageView.setFitWidth(bodyWidth);
                imageView.setFitHeight(IMAGE_HEIGHT);
            }
        }
    }

    private String formatMoney(NumberFormat money, BigDecimal value) {
        return money == null || value == null ? "0 VND" : money.format(value);
    }

    private String value(String value) {
        return value == null ? "" : value;
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

}
