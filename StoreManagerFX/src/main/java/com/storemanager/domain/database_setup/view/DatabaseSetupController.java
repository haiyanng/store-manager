package com.storemanager.domain.database_setup.view;

import com.storemanager.config.DatabaseSettings;
import com.storemanager.domain.database_setup.presenter.DatabaseSetupPresenter;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class DatabaseSetupController {

    @FXML
    private TextField hostField;

    @FXML
    private TextField portField;

    @FXML
    private TextField databaseField;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    private final DatabaseSetupPresenter presenter =
            new DatabaseSetupPresenter();

    @FXML
    public void initialize() {

        hostField.setText("localhost");
        portField.setText("3306");
        databaseField.setText("family_business_manager_db");
        usernameField.setText("root");
        passwordField.setText("");
    }

    @FXML
    public void onTestConnection() {

        DatabaseSettings settings =
                buildSettingsFromForm();

        boolean success =
                presenter.testConnection(settings);

        if (success) {

            showInfo(
                    "Connection successful",
                    "Connected to MySQL successfully."
            );

        } else {

            showError(
                    "Connection failed",
                    "Check the host, port, username and password, and make sure MySQL is running."
            );
        }
    }

    @FXML
    public void onSaveAndContinue() {

        DatabaseSettings settings =
                buildSettingsFromForm();

        boolean success =
                presenter.saveAndContinue(settings);

        if (!success) {

            showError(
                    "Unable to save configuration",
                    "Unable to connect to MySQL or save the configuration file."
            );
        }
    }

    private DatabaseSettings buildSettingsFromForm() {

        return new DatabaseSettings(
                hostField.getText().trim(),
                Integer.parseInt(
                        portField.getText().trim()
                ),
                databaseField.getText().trim(),
                usernameField.getText().trim(),
                passwordField.getText()
        );
    }

    private void showInfo(
            String title,
            String message
    ) {

        Alert alert =
                new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(
            String title,
            String message
    ) {

        Alert alert =
                new Alert(Alert.AlertType.ERROR);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}