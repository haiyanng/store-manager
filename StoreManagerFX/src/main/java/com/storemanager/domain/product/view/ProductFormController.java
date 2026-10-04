package com.storemanager.domain.product.view;

import com.storemanager.core.storage.ImageStorageService;
import com.storemanager.domain.category.model.Category;
import com.storemanager.domain.product.model.Product;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;

public class ProductFormController {

    private final ImageStorageService imageStorageService =
            new ImageStorageService();

    private String imagePath;

    @FXML
    private Button selectImageButton;

    @FXML
    private Button removeImageButton;

    @FXML
    private TextField nameField;

    @FXML
    private TextField skuField;


    @FXML
    private ComboBox<Category> categoryComboBox;

    @FXML
    private TextField basePriceField;

    @FXML
    private TextField unitField;

    @FXML
    private CheckBox activeCheckBox;

    @FXML
    private ImageView imagePreview;

    @FXML
    public void initialize() {

        activeCheckBox.setSelected(true);
    }

    @FXML
    private void onSelectImage() {

        FileChooser fileChooser =
                new FileChooser();

        fileChooser.setTitle("Choose product image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Image files",
                        "*.png",
                        "*.jpg",
                        "*.jpeg",
                        "*.gif",
                        "*.bmp"
                )
        );

        File selectedFile =
                fileChooser.showOpenDialog(
                        imagePreview.getScene().getWindow()
                );

        if (selectedFile == null) {
            return;
        }

        imagePath =
                imageStorageService.saveProductImage(
                        selectedFile
                );

        showImage(imagePath);
    }

    @FXML
    private void onRemoveImage() {

        imagePath = null;
        imagePreview.setImage(null);
    }

    public Product readProduct() {

        Product product =
                new Product();

        product.setName(nameField.getText());
        product.setSku(skuField.getText());

        Category category =
                categoryComboBox.getValue();

        product.setCategoryId(
                category == null
                        ? null
                        : category.getId()
        );

        product.setBasePrice(
                parseBasePrice()
        );

        product.setUnit(unitField.getText());
        product.setActive(activeCheckBox.isSelected());
        product.setImagePath(imagePath);

        return product;
    }

    public void showProduct(
            Product product
    ) {

        if (product == null) {
            clear();
            return;
        }

        nameField.setText(product.getName());
        skuField.setText(product.getSku());
        basePriceField.setText(
                product.getBasePrice() == null
                        ? "0"
                        : product.getBasePrice().toPlainString()
        );
        unitField.setText(product.getUnit());
        activeCheckBox.setSelected(product.isActive());
        imagePath = product.getImagePath();
        showImage(imagePath);

        selectCategory(
                product.getCategoryId()
        );
    }

    public void setCategories(
            List<Category> categories
    ) {

        categoryComboBox.setItems(
                FXCollections.observableArrayList(
                        categories
                )
        );
    }

    public void clear() {

        nameField.clear();
        skuField.clear();
        categoryComboBox.setValue(null);
        basePriceField.setText("0");
        unitField.clear();
        activeCheckBox.setSelected(true);
        imagePath = null;
        imagePreview.setImage(null);
    }

    public void setBusy(
            boolean busy
    ) {

        selectImageButton.setDisable(busy);
        removeImageButton.setDisable(busy);
        nameField.setDisable(busy);
        skuField.setDisable(busy);
        categoryComboBox.setDisable(busy);
        basePriceField.setDisable(busy);
        unitField.setDisable(busy);
        activeCheckBox.setDisable(busy);
    }

    private BigDecimal parseBasePrice() {

        String value =
                basePriceField.getText();

        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        return new BigDecimal(
                value.trim()
        );
    }

    private void selectCategory(
            Long categoryId
    ) {

        if (categoryId == null) {
            categoryComboBox.setValue(null);
            return;
        }

        categoryComboBox
                .getItems()
                .stream()
                .filter(category ->
                        categoryId.equals(category.getId())
                )
                .findFirst()
                .ifPresent(categoryComboBox::setValue);
    }

    private void showImage(
            String path
    ) {

        File imageFile =
                imageStorageService.resolveImageFile(path);

        if (imageFile == null || !imageFile.isFile()) {
            imagePreview.setImage(null);
            return;
        }

        imagePreview.setImage(
                new Image(
                        imageFile.toURI().toString(),
                        96,
                        96,
                        true,
                        true,
                        true
                )
        );
    }
}
