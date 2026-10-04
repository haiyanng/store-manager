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
    private boolean busy;

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
        if (busy) return;
        DatabaseSettings settings;
        try { settings = buildSettingsFromForm(); }
        catch (RuntimeException error) { showError("Invalid configuration", "Port must be a whole number from 1 to 65535."); return; }
        setBusy(true);
        presenter.testConnectionAsync(settings, success -> {
            if (success) showInfo("Connection successful", "Connected to MySQL successfully.");
            else showError("Connection failed", "Check the host, port, username and password, and make sure MySQL is running.");
        }, error -> showError("Connection failed", error.getMessage()), () -> setBusy(false));
    }

    @FXML
    public void onSaveAndContinue() {
        if (busy) return;
        DatabaseSettings settings;
        try { settings = buildSettingsFromForm(); }
        catch (RuntimeException error) { showError("Invalid configuration", "Port must be a whole number from 1 to 65535."); return; }
        setBusy(true);
        presenter.saveAndContinueAsync(settings, success -> {
            if (!success) showError("Unable to save configuration", "Unable to connect to MySQL or save the configuration file.");
        }, error -> showError("Unable to save configuration", error.getMessage()), () -> setBusy(false));
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        hostField.getParent().setDisable(busy);
    }

    private DatabaseSettings buildSettingsFromForm() {

        int port = Integer.parseInt(portField.getText().trim());
        if (port < 1 || port > 65535) throw new IllegalArgumentException("Invalid port");
        return new DatabaseSettings(
                hostField.getText().trim(),
                port,
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
