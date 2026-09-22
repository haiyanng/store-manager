package com.customershopfx.auth.view;

import com.customershopfx.app.SceneManager;
import com.customershopfx.app.SessionManager;
import com.customershopfx.auth.model.AuthResponse;
import com.customershopfx.auth.presenter.LoginPresenter;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class LoginController {
    @FXML private StackPane loginPanel;
    @FXML private VBox loginContent;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;
    @FXML private Button loginButton;
    @FXML private Button createAccountButton;

    private final LoginPresenter presenter = new LoginPresenter();

    @FXML
    private void initialize() {
        emailField.setText("customer@demo.com");
        passwordField.setText("123456");
    }

    public void resetView() {
        messageLabel.setText("");
        loginButton.setDisable(false);
        Platform.runLater(emailField::requestFocus);
    }

    @FXML
    private void login() {
        messageLabel.setText("Signing in...");
        loginButton.setDisable(true);
        presenter.login(emailField.getText(), passwordField.getText(), this::onLoginSuccess, this::onLoginError);
    }

    @FXML
    private void register() {
        SceneManager.showRegister();
    }

    private void onLoginSuccess(AuthResponse response) {
        SessionManager.start(response.token(), response.customer());
        SceneManager.showShop();
    }

    private void onLoginError(String message) {
        messageLabel.setText(message);
        loginButton.setDisable(false);
    }
}
