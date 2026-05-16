package com.storemanager.domain.auth.view;

import com.storemanager.core.navigation.SceneManager;
import com.storemanager.domain.auth.service.AuthService;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    private final AuthService authService =
            new AuthService();

    @FXML
    public void initialize() {

        passwordField.addEventFilter(
                InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
                Event::consume
        );
    }

    @FXML
    public void onLogin() {

        String username =
                usernameField.getText();

        String password =
                passwordField.getText();

        boolean success =
                authService.login(
                        username,
                        password
                );

        if (!success) {

            Alert alert =
                    new Alert(
                            Alert.AlertType.ERROR
                    );

            alert.setHeaderText(null);

            alert.setContentText(
                    "Sai tài khoản hoặc mật khẩu"
            );

            alert.showAndWait();

            return;
        }

        SceneManager.switchScene(
                "/fxml/dashboard/dashboard-shell.fxml"
        );
    }
}
