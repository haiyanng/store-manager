package com.storemanager.domain.auth.view;

import com.storemanager.core.util.UiFeedback;
import com.storemanager.core.navigation.SceneManager;
import com.storemanager.domain.auth.dto.LoginResponse;
import com.storemanager.domain.auth.presenter.LoginPresenter;
import com.storemanager.domain.license.model.LicenseState;
import com.storemanager.domain.license.model.LicenseStatus;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Label licenseStatusLabel;

    private final LoginPresenter presenter =
            new LoginPresenter();
    private boolean loginInProgress;

    @FXML
    public void initialize() {

        passwordField.addEventFilter(
                InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
                Event::consume
        );

        LicenseStatus licenseStatus =
                presenter.initialize();

        updateLicenseStatus(
                licenseStatus
        );
    }

    @FXML
    public void onLogin() {

        if (loginInProgress) return;
        String username = usernameField.getText();
        String password = passwordField.getText();
        loginInProgress = true;
        setBusy(true);
        presenter.loginAsync(username, password, response -> {
            if (!response.success()) { showError(response.message()); return; }
            SceneManager.switchScene("/fxml/dashboard/dashboard-shell.fxml");
        }, error -> showError(error.getMessage()), () -> {
            loginInProgress = false;
            setBusy(false);
        });
    }

    private void setBusy(boolean busy) {
        usernameField.setDisable(busy);
        passwordField.setDisable(busy);
        loginButton.setDisable(busy || presenter.getLicenseStatus().isBlocked());
        loginButton.setText(busy ? "Signing in..." : "Login");
    }

    private void updateLicenseStatus(
            LicenseStatus licenseStatus
    ) {

        if (licenseStatus == null || licenseStatusLabel == null) {
            return;
        }

        licenseStatusLabel.setText(
                licenseStatus.message()
        );

        licenseStatusLabel.getStyleClass().removeAll(
                "license-status-active",
                "license-status-warning",
                "license-status-blocked"
        );

        licenseStatusLabel.getStyleClass().add(
                "license-status"
        );

        if (licenseStatus.state() == LicenseState.ACTIVE) {
            licenseStatusLabel.getStyleClass().add(
                    "license-status-active"
            );
        } else if (licenseStatus.state() == LicenseState.GRACE_PERIOD) {
            licenseStatusLabel.getStyleClass().add(
                    "license-status-warning"
            );
        } else {
            licenseStatusLabel.getStyleClass().add(
                    "license-status-blocked"
            );
        }

        if (loginButton != null) {
            loginButton.setDisable(
                    licenseStatus.isBlocked()
            );
        }

        if (licenseStatus.isBlocked()) {
            showError(
                    licenseStatus.message()
            );
        }
    }

    private void showError(
            String message
    ) {
        UiFeedback.showError(message);
    }
}
