package com.customershopfx.shop.view;

import com.customershopfx.auth.model.Customer;
import com.customershopfx.cart.model.Cart;
import com.customershopfx.cart.model.CartItem;
import com.customershopfx.cart.presenter.CartPresenter;
import com.customershopfx.cart.viewmodel.CartViewModel;
import com.customershopfx.common.component.NotificationView;
import com.customershopfx.common.component.ProductCard;
import com.customershopfx.order.model.Order;
import com.customershopfx.order.presenter.OrderPresenter;
import com.customershopfx.order.viewmodel.OrderViewModel;
import com.customershopfx.profile.presenter.ProfilePresenter;
import com.customershopfx.profile.viewmodel.ProfileViewModel;
import com.customershopfx.product.model.Category;
import com.customershopfx.product.model.Product;
import com.customershopfx.product.presenter.ProductPresenter;
import com.customershopfx.product.viewmodel.ProductViewModel;
import com.customershopfx.shop.presenter.ShopPresenter;
import com.customershopfx.shop.viewmodel.ShopViewModel;
import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ShopController {
    private static final double PRODUCT_TILE_MIN_WIDTH = 240;
    private static final double PRODUCT_TILE_MAX_WIDTH = 320;
    private static final double PRODUCT_GRID_GAP = 16;
    private static final int PRODUCT_GRID_MAX_COLUMNS = 5;

    @FXML private StackPane pageHost;
    @FXML private VBox productsPage;
    @FXML private HBox cartPage;
    @FXML private VBox checkoutPage;
    @FXML private VBox ordersPage;
    @FXML private VBox profilePage;
    @FXML private VBox shopMain;
    @FXML private VBox detailPanel;
    @FXML private Label detailTitleLabel;
    @FXML private Label detailMessageLabel;
    @FXML private VBox detailContentBox;
    @FXML private VBox detailBodyBox;
    @FXML private Label statusLabel;
    @FXML private NotificationView notificationView;
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Label customerNameLabel;
    @FXML private Label cartCountLabel;
    @FXML private Button logoutButton;
    @FXML private Button productsButton;
    @FXML private Button cartNavButton;
    @FXML private Button checkoutButton;
    @FXML private Button ordersButton;
    @FXML private Button profileButton;
    @FXML private ComboBox<Category> categoryCombo;
    @FXML private ComboBox<String> sortCombo;
    @FXML private FlowPane productGrid;
    @FXML private VBox cartList;
    @FXML private Label cartTotalLabel;
    @FXML private TextField checkoutNameField;
    @FXML private TextField checkoutPhoneField;
    @FXML private TextArea checkoutAddressField;
    @FXML private ComboBox<String> paymentCombo;
    @FXML private Label checkoutMessageLabel;
    @FXML private ListView<Order> ordersList;
    @FXML private TextField profileNameField;
    @FXML private TextField profilePhoneField;
    @FXML private TextField profileEmailField;
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;

    private final ShopViewModel shopViewModel = new ShopViewModel();
    private final ProductViewModel productViewModel = new ProductViewModel();
    private final CartViewModel cartViewModel = new CartViewModel();
    private final OrderViewModel orderViewModel = new OrderViewModel();
    private final ProfileViewModel profileViewModel = new ProfileViewModel();
    private final javafx.beans.property.StringProperty customerDisplayName = new javafx.beans.property.SimpleStringProperty("Customer");
    private final NumberFormat money = NumberFormat.getCurrencyInstance(Locale.US);
    private Long selectedProductId;
    private ShopPresenter shopPresenter;
    private ProductPresenter productPresenter;
    private CartPresenter cartPresenter;
    private OrderPresenter orderPresenter;
    private ProfilePresenter profilePresenter;
    private final ChangeListener<Number> selectedTabListener = (obs, old, index) -> {
        int tab = index == null ? 0 : index.intValue();
        showPage(tab);
        updateNavState(tab);
    };

    @FXML
    private void initialize() {
        cartPresenter = new CartPresenter(this, cartViewModel, shopViewModel::setStatusMessage);
        shopPresenter = new ShopPresenter(this, shopViewModel);
        productPresenter = new ProductPresenter(this, productViewModel, shopViewModel::setStatusMessage, cartPresenter);
        orderPresenter = new OrderPresenter(this, orderViewModel, shopViewModel::setStatusMessage, cartPresenter);
        profilePresenter = new ProfilePresenter(profileViewModel, shopViewModel::setStatusMessage,
                profile -> orderPresenter.syncCheckout(profile.fullName(), profile.phone(),
                        orderViewModel.getCheckoutAddress(), orderViewModel.getPaymentMethod()),
                customer -> showCustomer(customer));

        statusLabel.textProperty().bind(shopViewModel.statusMessageProperty());
        cartTotalLabel.textProperty().bind(Bindings.createStringBinding(
                () -> money.format(cartViewModel.getTotal()), cartViewModel.totalProperty()));
        checkoutNameField.textProperty().bindBidirectional(orderViewModel.checkoutNameProperty());
        checkoutPhoneField.textProperty().bindBidirectional(orderViewModel.checkoutPhoneProperty());
        checkoutAddressField.textProperty().bindBidirectional(orderViewModel.checkoutAddressProperty());
        paymentCombo.valueProperty().bindBidirectional(orderViewModel.paymentMethodProperty());
        checkoutMessageLabel.textProperty().bind(orderViewModel.checkoutMessageProperty());
        ordersList.setItems(orderViewModel.getOrders());
        profileEmailField.textProperty().bind(profileViewModel.emailProperty());
        profileNameField.textProperty().bindBidirectional(profileViewModel.fullNameProperty());
        profilePhoneField.textProperty().bindBidirectional(profileViewModel.phoneProperty());
        currentPasswordField.textProperty().bindBidirectional(profileViewModel.currentPasswordProperty());
        newPasswordField.textProperty().bindBidirectional(profileViewModel.newPasswordProperty());
        searchField.textProperty().bindBidirectional(productViewModel.keywordProperty());
        customerNameLabel.textProperty().bind(customerDisplayNameProperty());
        cartCountLabel.textProperty().bind(cartViewModel.itemCountProperty().asString());

        sortCombo.valueProperty().bindBidirectional(productViewModel.selectedSortProperty());
        categoryCombo.valueProperty().bindBidirectional(productViewModel.selectedCategoryProperty());
        categoryCombo.valueProperty().addListener((obs, old, value) -> syncCategorySelectionStyle());

        sortCombo.getItems().setAll("name_asc", "name_desc", "price_asc", "price_desc");
        sortCombo.getSelectionModel().selectFirst();
        paymentCombo.getItems().setAll("Cash on delivery", "Card on delivery");
        paymentCombo.getSelectionModel().selectFirst();
        ordersList.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Order order, boolean empty) {
                super.updateItem(order, empty);
                setText(empty || order == null ? null : "#" + order.id() + "  " + order.status() + "  " + money.format(order.totalAmount()));
            }
        });
        ordersList.getSelectionModel().selectedItemProperty().addListener((obs, old, order) -> orderPresenter.onOrderSelected(order));
        configureProductGrid();
        shopViewModel.selectedTabProperty().addListener(selectedTabListener);
        showPage(shopViewModel.getSelectedTab());
        updateNavState(shopViewModel.getSelectedTab());

        productPresenter.initialize();
        cartPresenter.initialize();
        orderPresenter.initialize();
        profilePresenter.initialize();
        shopPresenter.initialize();
        syncCategorySelectionStyle();

    }

    @FXML
    public void loadProducts() {
        productPresenter.loadProducts();
    }

    @FXML
    public void onProductsTab() {
        selectTab(0);
    }

    @FXML
    public void onCartTab() {
        selectTab(1);
    }

    @FXML
    public void onCheckoutTab() {
        selectTab(2);
    }

    @FXML
    public void onOrdersTab() {
        selectTab(3);
    }

    @FXML
    public void onProfileTab() {
        selectTab(4);
    }

    @FXML
    public void refreshCart() {
        cartPresenter.loadCart();
    }

    @FXML
    public void loadOrders() {
        orderPresenter.loadOrders();
    }

    @FXML
    public void placeOrder() {
        orderPresenter.placeOrder();
    }

    @FXML
    public void saveProfile() {
        profilePresenter.updateProfile();
    }

    @FXML
    public void changePassword() {
        profilePresenter.changePassword();
    }

    @FXML
    public void clearCart() {
        cartPresenter.clearCart();
    }

    @FXML
    public void logout() {
        shopPresenter.logout();
    }

    @FXML
    public void showSupportInfo() {
        showFooterDialog(
                "Support",
                "For order support, contact the store team or review your order timeline in the Orders tab."
        );
    }

    @FXML
    public void showTermsInfo() {
        showFooterDialog(
                "Terms",
                "Orders are confirmed by store staff. Availability, delivery timing, and payment terms may vary by branch."
        );
    }

    @FXML
    public void showPrivacyInfo() {
        showFooterDialog(
                "Privacy",
                "Customer profile and order details are used only for checkout, delivery, and order history."
        );
    }

    public void selectTab(int index) {
        shopViewModel.setSelectedTab(index);
    }

    public IntegerProperty selectedTabProperty() {
        return shopViewModel.selectedTabProperty();
    }

    public int getSelectedTab() {
        return shopViewModel.getSelectedTab();
    }

    public void showCustomer(Customer customer) {
        customerDisplayName.set(customer == null ? "Customer" : customer.fullName());
    }

    public javafx.beans.property.StringProperty customerDisplayNameProperty() {
        return customerDisplayName;
    }

    public javafx.beans.property.StringProperty searchTextProperty() {
        return productViewModel.keywordProperty();
    }

    public IntegerProperty cartCountProperty() {
        return cartViewModel.itemCountProperty();
    }

    public void renderCategories(List<Category> categories) {
        categoryCombo.getItems().clear();
        categoryCombo.getItems().add(new Category(null, "All categories", null));
        categoryCombo.getItems().addAll(categories);
        categoryCombo.getSelectionModel().selectFirst();
        syncCategorySelectionStyle();
    }

    public void renderProducts(List<Product> products) {
        productGrid.getChildren().clear();
        boolean needsDefaultSelection = selectedProductId == null && !products.isEmpty();
        if (needsDefaultSelection) {
            selectedProductId = products.get(0).id();
        }
        for (Product product : products) {
            ProductCard card = new ProductCard(product, money,
                    selectedProductId != null && selectedProductId.equals(product.id()),
                    selected -> {
                        selectedProductId = selected.id();
                        productPresenter.showProductDetail(selected);
                    },
                    (selected, quantity) -> productPresenter.addToCart(selected.id(), quantity, selected.name()));
            productGrid.getChildren().add(card);
        }
        updateProductTileLayout();
        if (needsDefaultSelection) {
            renderProductDetailContent(products.get(0), false);
        }
    }

    private void configureProductGrid() {
        productGrid.setAlignment(Pos.TOP_LEFT);
        productGrid.setMinWidth(0);
        productGrid.setMaxWidth(Double.MAX_VALUE);
        productGrid.setHgap(PRODUCT_GRID_GAP);
        productGrid.setVgap(PRODUCT_GRID_GAP);

        ChangeListener<Number> layoutListener =
                (obs, oldValue, newValue) -> updateProductTileLayout();
        productGrid.widthProperty().addListener(layoutListener);
        if (shopMain != null) {
            shopMain.widthProperty().addListener(layoutListener);
        }
        Platform.runLater(this::updateProductTileLayout);
    }

    private void updateProductTileLayout() {
        if (productGrid == null) {
            return;
        }

        double availableWidth =
                productGrid.getWidth();
        if (availableWidth <= 0 && shopMain != null) {
            availableWidth =
                    shopMain.getWidth();
        }
        if (availableWidth <= 0) {
            return;
        }

        double horizontalPadding =
                productGrid.getPadding().getLeft()
                        + productGrid.getPadding().getRight();
        double usableWidth =
                Math.max(
                        PRODUCT_TILE_MIN_WIDTH,
                        availableWidth - horizontalPadding
                );
        int columns =
                calculateProductColumns(usableWidth);
        double tileWidth =
                (usableWidth - ((columns - 1) * PRODUCT_GRID_GAP)) / columns;
        tileWidth =
                Math.max(
                        PRODUCT_TILE_MIN_WIDTH,
                        Math.min(PRODUCT_TILE_MAX_WIDTH, Math.floor(tileWidth) - 1)
                );

        productGrid.setPrefWrapLength(usableWidth);
        for (Node child : productGrid.getChildren()) {
            if (child instanceof ProductCard productCard) {
                productCard.setTileWidth(tileWidth);
            }
        }
    }

    private int calculateProductColumns(double availableWidth) {
        for (int columns = PRODUCT_GRID_MAX_COLUMNS; columns > 1; columns--) {
            double requiredWidth =
                    (columns * PRODUCT_TILE_MIN_WIDTH)
                            + ((columns - 1) * PRODUCT_GRID_GAP);
            if (availableWidth >= requiredWidth) {
                return columns;
            }
        }
        return 1;
    }

    public void renderCart(Cart cart) {
        cartList.getChildren().clear();
        if (cart.items().isEmpty()) {
            cartList.getChildren().add(label("Your cart is empty", "empty-state"));
        }
        for (CartItem item : cart.items()) {
            Label name = new Label(item.productName());
            name.getStyleClass().addAll("text-card-title", "product-title");
            HBox.setHgrow(name, Priority.ALWAYS);
            Spinner<Integer> qty = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99, item.quantity()));
            qty.getStyleClass().addAll("large", "rounded");
            qty.valueProperty().addListener((obs, old, value) -> cartPresenter.updateQuantity(item.id(), value));
            Button remove = new Button("Remove");
            remove.getStyleClass().addAll("button", Styles.BUTTON_OUTLINED, Styles.ROUNDED, Styles.LARGE, Styles.DANGER);
            remove.setOnAction(event -> cartPresenter.removeItem(item.id()));
            HBox actions = new HBox(8, qty, remove);
            Label subtotal = new Label(money.format(item.subtotal()));
            subtotal.getStyleClass().addAll("text-price", "product-price");
            VBox details = new VBox(8, name, subtotal, actions);
            HBox row = new HBox(14, productImage(item.productName(), item.imagePath(), 92, 76), details);
            row.getStyleClass().addAll("panel", "cart-card", "cart-row");
            cartList.getChildren().add(row);
        }
    }

    public void renderOrders(List<Order> orders) {
        ordersList.getItems().setAll(orders);
    }

    public void renderOrderDetail(Order order) {
        if (order == null) {
            showDetailPlaceholder("Select an order", "Choose an order to view items and status.");
            return;
        }
        Button cancel = new Button("Cancel pending order");
        cancel.getStyleClass().addAll("button", Styles.BUTTON_OUTLINED, Styles.ROUNDED, Styles.LARGE, Styles.DANGER);
        cancel.setDisable(!"PENDING".equals(order.status()));
        cancel.setOnAction(event -> orderPresenter.cancelOrder(order.id()));
        detailTitleLabel.setText("Order #" + order.id());
        detailMessageLabel.setText("Status: " + order.status());
        detailBodyBox.getChildren().setAll(
                label("Total: " + money.format(order.totalAmount()), "text-price"),
                label("Ship to: " + order.shippingAddress(), "text-meta"),
                cancel
        );
        order.items().forEach(item -> detailBodyBox.getChildren().add(
                label(item.productName() + " x" + item.quantity() + " - " + money.format(item.subtotal()), "text-body")));
        if (order.history() != null && !order.history().isEmpty()) {
            detailBodyBox.getChildren().add(label("History", "text-section"));
            order.history().forEach(entry -> {
                String note =
                        entry.note() == null || entry.note().isBlank()
                                ? ""
                                : " - " + entry.note();
                detailBodyBox.getChildren().add(
                        label(entry.createdAt() + " | " + entry.oldStatus() + " -> " + entry.newStatus() + note, "text-meta")
                );
            });
        }
    }

    public void showCheckoutMessage(String message) {
        checkoutMessageLabel.setText(message);
    }

    public void showCartNotificationSuccess(String productName, int quantity) {
        if (notificationView != null) {
            notificationView.showSuccess("Added to cart", productName + " x" + quantity + " has been added.");
        }
    }

    public void showToastError(String message) {
        if (notificationView != null) {
            notificationView.showError("Cart update failed", "Could not add item. Please try again.");
        }
    }

    public String getCheckoutName() {
        return checkoutNameField.getText();
    }

    public String getCheckoutPhone() {
        return checkoutPhoneField.getText();
    }

    public String getCheckoutAddress() {
        return checkoutAddressField.getText();
    }

    public String getPaymentMethod() {
        return paymentCombo.getValue();
    }

    public void renderProductDetail(Product product) {
        renderProductDetailContent(product, true);
    }

    private void renderProductDetailContent(Product product, boolean rerenderGrid) {
        selectedProductId = product.id();
        Spinner<Integer> quantitySpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99, 1));
        quantitySpinner.setMaxWidth(96);
        quantitySpinner.getStyleClass().addAll("large", "rounded");

        Button add = new Button("Add to Cart");
        add.setMaxWidth(Double.MAX_VALUE);
        add.getStyleClass().addAll("button", Styles.ACCENT, Styles.ROUNDED, Styles.LARGE);
        add.setOnAction(event -> productPresenter.addToCart(product.id(), quantitySpinner.getValue(), product.name()));
        add.setOnMouseClicked(event -> event.consume());

        HBox quantityRow = new HBox(10,
                label("Quantity", "text-meta"),
                quantitySpinner);
        quantityRow.setAlignment(Pos.CENTER_LEFT);

        Label name = label(product.name(), "product-detail-name");
        name.setWrapText(true);

        Label category = label("Category: " + categoryName(product), "text-meta");
        category.setWrapText(true);

        Label metadata = label(
                "Unit: " + safe(product.unit())
                        + "  |  SKU: " + safe(product.sku())
                        + "  |  Barcode: " + safe(product.barcode()),
                "text-meta"
        );
        metadata.setWrapText(true);

        Label descriptionHeading = label("Description", "product-description-heading");
        Label fullDescription = label(descriptionText(product), "product-full-description");
        fullDescription.setWrapText(true);

        VBox contentBlock = new VBox(10,
                productImage(product.name(), product.imagePath(), 236, 150),
                name,
                category,
                label(money.format(product.basePrice()), "text-price"),
                metadata,
                descriptionHeading,
                new Separator(),
                fullDescription,
                new Separator(),
                quantityRow,
                add);
        contentBlock.setAlignment(Pos.CENTER);
        contentBlock.setFillWidth(true);
        contentBlock.setMaxWidth(Double.MAX_VALUE);

        detailTitleLabel.setText(product.name());
        detailMessageLabel.setText("Category: " + categoryName(product));
        detailBodyBox.setAlignment(Pos.CENTER);
        detailBodyBox.getChildren().setAll(contentBlock);
        if (rerenderGrid) {
            renderProducts(productViewModel.getProducts());
        }
    }

    private String categoryName(Product product) {
        return product.categoryName() == null || product.categoryName().isBlank()
                ? "General"
                : product.categoryName();
    }

    private String descriptionText(Product product) {
        return product.fullDescription() == null || product.fullDescription().isBlank()
                ? "No description available."
                : product.fullDescription();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }

    private void syncCategorySelectionStyle() {
        if (categoryCombo == null) {
            return;
        }
        categoryCombo.getStyleClass().remove("category-chip-active");
        Category selected = categoryCombo.getValue();
        if (selected != null && selected.id() != null) {
            categoryCombo.getStyleClass().add("category-chip-active");
        }
    }

    private void showPage(int index) {
        setPageVisible(productsPage, index == 0);
        setPageVisible(cartPage, index == 1);
        setPageVisible(checkoutPage, index == 2);
        setPageVisible(ordersPage, index == 3);
        setPageVisible(profilePage, index == 4);
        if (index == 3) {
            loadOrders();
        }
        switch (index) {
            case 1 -> showDetailPlaceholder("Cart", "Review items on the left and continue to checkout.");
            case 2 -> showDetailPlaceholder("Checkout", "Confirm shipping details before placing the order.");
            case 3 -> showDetailPlaceholder("Select an order", "Choose an order to view items and status.");
            case 4 -> showDetailPlaceholder("Profile", "Update account and password settings.");
            default -> showDetailPlaceholder("Select a product", "Click a product card to view details.");
        }
    }

    private void updateNavState(int index) {
        if (productsButton != null) {
            productsButton.getStyleClass().remove("shop-nav-button-active");
            cartNavButton.getStyleClass().remove("shop-nav-button-active");
            checkoutButton.getStyleClass().remove("shop-nav-button-active");
            ordersButton.getStyleClass().remove("shop-nav-button-active");
            profileButton.getStyleClass().remove("shop-nav-button-active");
            switch (index) {
                case 1 -> cartNavButton.getStyleClass().add("shop-nav-button-active");
                case 2 -> checkoutButton.getStyleClass().add("shop-nav-button-active");
                case 3 -> ordersButton.getStyleClass().add("shop-nav-button-active");
                case 4 -> profileButton.getStyleClass().add("shop-nav-button-active");
                default -> productsButton.getStyleClass().add("shop-nav-button-active");
            }
        }
    }

    private void showDetailPlaceholder(String title, String message) {
        if (detailTitleLabel != null) {
            detailTitleLabel.setText(title);
        }
        if (detailMessageLabel != null) {
            detailMessageLabel.setText(message);
        }
        if (detailBodyBox != null) {
            detailBodyBox.getChildren().clear();
        }
    }

    private void showFooterDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void setPageVisible(Node node, boolean visible) {
        if (node == null) {
            return;
        }
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private Label label(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
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
