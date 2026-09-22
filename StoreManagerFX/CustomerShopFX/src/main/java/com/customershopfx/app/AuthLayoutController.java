package com.customershopfx.app;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

public class AuthLayoutController {
    private static final String LOGIN_BACKGROUND = "-fx-background-color: linear-gradient(to bottom right, #1F2937 0%, #7C2D12 40%, #EA580C 72%, #F59E0B 100%); -fx-background-insets: 0;";
    private static final String REGISTER_BACKGROUND = "-fx-background-color: linear-gradient(to bottom right, #0F172A 0%, #1E3A8A 55%, #2563EB 100%); -fx-background-insets: 0;";

    @FXML private StackPane backgroundLayer;
    @FXML private StackPane contentLayer;

    private Parent loginView;
    private Parent registerView;
    private com.customershopfx.auth.view.LoginController loginController;
    private com.customershopfx.auth.view.RegisterController registerController;
    private UiMode mode = UiMode.LOGIN;

    public void setViews(Parent loginView,
                         Parent registerView,
                         com.customershopfx.auth.view.LoginController loginController,
                         com.customershopfx.auth.view.RegisterController registerController) {
        this.loginView = loginView;
        this.registerView = registerView;
        this.loginController = loginController;
        this.registerController = registerController;
        contentLayer.getChildren().setAll(loginView, registerView);
        applyMode();
    }

    public void showLogin() {
        mode = UiMode.LOGIN;
        applyMode();
        if (loginController != null) {
            loginController.resetView();
        }
    }

    public void showRegister() {
        mode = UiMode.REGISTER;
        applyMode();
        if (registerController != null) {
            registerController.resetView();
        }
    }

    private void applyMode() {
        if (backgroundLayer != null) {
            backgroundLayer.setStyle(mode == UiMode.LOGIN ? LOGIN_BACKGROUND : REGISTER_BACKGROUND);
        }
        if (loginView != null) {
            boolean loginVisible = mode == UiMode.LOGIN;
            loginView.setVisible(loginVisible);
            loginView.setManaged(loginVisible);
        }
        if (registerView != null) {
            boolean registerVisible = mode == UiMode.REGISTER;
            registerView.setVisible(registerVisible);
            registerView.setManaged(registerVisible);
        }
    }

    private enum UiMode {
        LOGIN,
        REGISTER
    }
}
