package com.storemanager.domain.category.view;

import com.storemanager.core.storage.ImageStorageService;
import com.storemanager.domain.category.model.Category;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;

public class CategoryFormController {

    private final ImageStorageService imageStorageService =
            new ImageStorageService();

    private String imagePath;

    @FXML
    private TextField nameField;

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

        fileChooser.setTitle("Choose category image");
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
                imageStorageService.saveCategoryImage(
                        selectedFile
                );

        showImage(imagePath);
    }

    @FXML
    private void onRemoveImage() {

        imagePath = null;
        imagePreview.setImage(null);
    }

    public Category readCategory() {

        Category category =
                new Category();

        category.setName(nameField.getText());
        category.setActive(activeCheckBox.isSelected());
        category.setImagePath(imagePath);

        return category;
    }

    public void showCategory(
            Category category
    ) {

        if (category == null) {
            clear();
            return;
        }

        nameField.setText(category.getName());
        activeCheckBox.setSelected(category.isActive());
        imagePath = category.getImagePath();
        showImage(imagePath);
    }

    public void clear() {

        nameField.clear();
        activeCheckBox.setSelected(true);
        imagePath = null;
        imagePreview.setImage(null);
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
