package com.storemanager.domain.auth.view;

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
import javafx.scene.control.Alert;
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

        LoginResponse response =
                presenter.login(
                        usernameField.getText(),
                        passwordField.getText()
                );

        if (!response.success()) {
            showError(
                    response.message()
            );
            return;
        }

        SceneManager.switchScene(
                "/fxml/dashboard/dashboard-shell.fxml"
        );
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

        Alert alert =
                new Alert(
                        Alert.AlertType.ERROR
                );

        alert.setHeaderText(
                null
        );

        alert.setContentText(
                message
        );

        alert.showAndWait();
    }
}
