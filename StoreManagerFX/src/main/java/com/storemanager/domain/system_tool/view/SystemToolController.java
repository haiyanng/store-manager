package com.storemanager.domain.system_tool.view;

import com.storemanager.domain.system_tool.presenter.SystemToolPresenter;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import java.io.File;

public class SystemToolController {

    @FXML
    private TextField mysqlPathField;

    @FXML
    private TextField mysqldumpPathField;

    @FXML
    private Button backupButton;

    @FXML
    private Button restoreButton;

    @FXML
    private Label statusLabel;

    private SystemToolPresenter presenter;

    @FXML
    public void initialize() {

        presenter =
                new SystemToolPresenter(
                        this
                );

        mysqlPathField.setText("mysql");
        mysqldumpPathField.setText("mysqldump");
        presenter.initialize();
    }

    @FXML
    public void onBackup() {

        FileChooser fileChooser =
                new FileChooser();

        fileChooser.setTitle(
                "Choose backup SQL file"
        );

        fileChooser.setInitialFileName(
                presenter.createBackupFileName()
        );

        fileChooser
                .getExtensionFilters()
                .add(
                        new FileChooser.ExtensionFilter(
                                "SQL files",
                                "*.sql"
                        )
                );

        File outputFile =
                fileChooser.showSaveDialog(
                        backupButton.getScene().getWindow()
                );

        if (outputFile == null) {
            return;
        }

        presenter.backup(
                mysqldumpPathField.getText(),
                outputFile
        );
    }

    @FXML
    public void onRestore() {

        FileChooser fileChooser =
                new FileChooser();

        fileChooser.setTitle(
                "Choose SQL restore file"
        );

        fileChooser
                .getExtensionFilters()
                .add(
                        new FileChooser.ExtensionFilter(
                                "SQL files",
                                "*.sql"
                        )
                );

        File inputFile =
                fileChooser.showOpenDialog(
                        restoreButton.getScene().getWindow()
                );

        if (inputFile == null) {
            return;
        }

        Alert confirm =
                new Alert(
                        Alert.AlertType.CONFIRMATION
                );

        confirm.setHeaderText("Restore database?");
        confirm.setContentText(
                "This will import the selected SQL file into the current database."
        );

        if (confirm.showAndWait().orElse(ButtonType.CANCEL)
                != ButtonType.OK) {
            return;
        }

        presenter.restore(
                mysqlPathField.getText(),
                inputFile
        );
    }

    public void setBusy(
            boolean busy
    ) {

        backupButton.setDisable(busy);
        restoreButton.setDisable(busy);
        mysqlPathField.setDisable(busy);
        mysqldumpPathField.setDisable(busy);
    }

    public void setStatus(
            String status
    ) {

        statusLabel.setText(status);
    }

    public void showInfo(
            String message
    ) {

        Alert alert =
                new Alert(
                        Alert.AlertType.INFORMATION
                );

        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void showError(
            String message
    ) {

        Alert alert =
                new Alert(
                        Alert.AlertType.ERROR
                );

        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
