package com.storeapi.setup.view;

import com.storeapi.setup.presenter.DatabaseSetupPresenter;
import com.storeapi.setup.service.DatabaseConfigFileService;
import com.storeapi.setup.service.DatabaseConnectionTestService;
import com.storeapi.setup.viewmodel.DatabaseSetupViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class DatabaseSetupController {
    @FXML private TextField hostField;
    @FXML private TextField portField;
    @FXML private TextField databaseField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField serverPortField;
    @FXML private Label statusLabel;
    @FXML private Button testConnectionButton;
    @FXML private Button saveConfigButton;
    @FXML private Button startApiButton;

    private final DatabaseSetupViewModel viewModel = new DatabaseSetupViewModel();
    private final DatabaseSetupPresenter presenter = new DatabaseSetupPresenter(
            viewModel,
            new DatabaseConfigFileService(),
            new DatabaseConnectionTestService()
    );

    @FXML
    private void initialize() {
        hostField.textProperty().bindBidirectional(viewModel.hostProperty());
        portField.textProperty().bindBidirectional(viewModel.portProperty());
        databaseField.textProperty().bindBidirectional(viewModel.databaseNameProperty());
        usernameField.textProperty().bindBidirectional(viewModel.usernameProperty());
        passwordField.textProperty().bindBidirectional(viewModel.passwordProperty());
        serverPortField.textProperty().bindBidirectional(viewModel.serverPortProperty());
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());
        testConnectionButton.disableProperty().bind(viewModel.busyProperty());
        saveConfigButton.disableProperty().bind(viewModel.busyProperty());
        startApiButton.disableProperty().bind(viewModel.busyProperty());
        presenter.initialize();
    }

    @FXML
    private void testConnection() {
        presenter.testConnection();
    }

    @FXML
    private void saveConfig() {
        presenter.saveConfig();
    }

    @FXML
    private void startApi() {
        presenter.startApi();
    }
}
