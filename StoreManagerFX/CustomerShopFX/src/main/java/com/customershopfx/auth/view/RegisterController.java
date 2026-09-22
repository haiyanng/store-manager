package com.customershopfx.auth.view;

import com.customershopfx.app.SceneManager;
import com.customershopfx.app.SessionManager;
import com.customershopfx.auth.model.AuthResponse;
import com.customershopfx.auth.presenter.RegisterPresenter;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {
    @FXML private StackPane registerPanel;
    @FXML private VBox registerContent;
    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;
    @FXML private Button registerButton;
    @FXML private Button backButton;

    private final RegisterPresenter presenter = new RegisterPresenter();

    @FXML
    private void initialize() {
    }

    public void resetView() {
        messageLabel.setText("");
        registerButton.setDisable(false);
        backButton.setDisable(false);
        nameField.clear();
        phoneField.clear();
        emailField.clear();
        passwordField.clear();
        Platform.runLater(nameField::requestFocus);
    }

    @FXML
    private void register() {
        messageLabel.setText("Creating account...");
        registerButton.setDisable(true);
        presenter.register(emailField.getText(), passwordField.getText(), nameField.getText(), phoneField.getText(),
                this::onRegisterSuccess, this::onRegisterError);
    }

    @FXML
    private void backToLogin() {
        SceneManager.showLogin();
    }

    private void onRegisterSuccess(AuthResponse response) {
        SessionManager.start(response.token(), response.customer());
        SceneManager.showShop();
    }

    private void onRegisterError(String message) {
        messageLabel.setText(message);
        registerButton.setDisable(false);
    }
}
