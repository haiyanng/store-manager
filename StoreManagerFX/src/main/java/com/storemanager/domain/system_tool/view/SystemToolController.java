package com.storemanager.domain.system_tool.view;

import com.storemanager.core.util.UiFeedback;
import com.storemanager.domain.system_tool.presenter.SystemToolPresenter;
import com.storemanager.domain.system_tool.backup.model.BackupSummary;
import com.storemanager.core.util.TimeFormatUtil;
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
    private Label latestBackupTimeLabel;

    @FXML
    private Label latestBackupSizeLabel;

    @FXML
    private Label backupLocationLabel;

    @FXML
    private Label restoreWarningLabel;

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
                "Save complete backup (database and images)"
        );

        fileChooser.setInitialFileName(
                presenter.createBackupFileName()
        );

        fileChooser
                .getExtensionFilters()
                .add(
                        new FileChooser.ExtensionFilter(
                                "Complete backup ZIP files",
                                "*.zip"
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
                "Choose complete backup or legacy SQL backup"
        );

        fileChooser
                .getExtensionFilters()
                .add(
                        new FileChooser.ExtensionFilter(
                                "Backup files",
                                "*.zip", "*.sql"
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
                inputFile.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".zip")
                        ? "Restore replaces the current database and product, category and employee images."
                        : "This legacy SQL backup restores the database only. Images are not included."
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

        com.storemanager.core.util.UiFeedback.status(statusLabel, status);
    }

    public void setBackupSummary(
            BackupSummary summary
    ) {

        if (summary == null) {
            latestBackupTimeLabel.setText("No backup yet");
            latestBackupSizeLabel.setText("Size: -");
            backupLocationLabel.setText("Location: -");
            return;
        }

        latestBackupTimeLabel.setText(
                summary.getCreatedAt() == null
                        ? "No backup time"
                        : "Latest: "
                                + TimeFormatUtil.formatDateTime(
                                        summary.getCreatedAt()
                                )
        );
        latestBackupSizeLabel.setText(
                "Size: " + formatBytes(summary.getSizeBytes())
        );
        backupLocationLabel.setText(
                "Location: " + summary.getLocation()
        );
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
        UiFeedback.showError(message);
    }

    private String formatBytes(
            long sizeBytes
    ) {

        if (sizeBytes < 1024) {
            return sizeBytes + " B";
        }

        long kb = sizeBytes / 1024;

        if (kb < 1024) {
            return kb + " KB";
        }

        long mb = kb / 1024;

        return mb + " MB";
    }
}
