package com.storemanager.domain.employee.view;

import com.storemanager.core.storage.ImageStorageService;
import com.storemanager.domain.employee.model.Employee;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;

public class EmployeeFormController {

    private final ImageStorageService imageStorageService =
            new ImageStorageService();

    private String imagePath;

    @FXML
    private TextField fullNameField;

    @FXML
    private TextField phoneField;

    @FXML
    private TextField addressField;

    @FXML
    private TextField positionField;

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

        fileChooser.setTitle("Choose employee image");
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
                imageStorageService.saveEmployeeImage(
                        selectedFile
                );

        showImage(imagePath);
    }

    @FXML
    private void onRemoveImage() {

        imagePath = null;
        imagePreview.setImage(null);
    }

    public Employee readEmployee() {

        Employee employee =
                new Employee();

        employee.setFullName(
                fullNameField.getText()
        );

        employee.setPhone(
                phoneField.getText()
        );

        employee.setAddress(
                addressField.getText()
        );

        employee.setPosition(
                positionField.getText()
        );

        employee.setActive(
                activeCheckBox.isSelected()
        );

        employee.setImagePath(imagePath);

        return employee;
    }

    public void showEmployee(
            Employee employee
    ) {

        if (employee == null) {
            clear();
            return;
        }

        fullNameField.setText(
                employee.getFullName()
        );

        phoneField.setText(
                employee.getPhone()
        );

        addressField.setText(
                employee.getAddress()
        );

        positionField.setText(
                employee.getPosition()
        );

        activeCheckBox.setSelected(
                employee.isActive()
        );

        imagePath =
                employee.getImagePath();

        showImage(imagePath);
    }

    public void clear() {

        fullNameField.clear();
        phoneField.clear();
        addressField.clear();
        positionField.clear();
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
