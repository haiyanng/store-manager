package com.storemanager.domain.dashboard.view;

import com.storemanager.core.navigation.SceneManager;
import com.storemanager.core.session.AppSession;
import com.storemanager.domain.auth.service.AuthService;
import com.storemanager.domain.user.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class DashboardController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label roleLabel;

    private final AuthService authService =
            new AuthService();

    @FXML
    public void initialize() {

        User currentUser =
                AppSession.getCurrentUser();

        if (currentUser == null) {

            SceneManager.switchScene(
                    "/fxml/auth/login.fxml"
            );

            return;
        }

        welcomeLabel.setText(
                "Welcome "
                        + currentUser.getUsername()
        );

        roleLabel.setText(
                "Role: "
                        + currentUser.getRole()
        );
    }

    @FXML
    public void onLogout() {

        authService.logout();

        SceneManager.switchScene(
                "/fxml/auth/login.fxml"
        );
    }
}