package com.storemanager.domain.system_tool.migration_export.view;

import com.storemanager.core.util.UiFeedback;
import com.storemanager.domain.offline_export.dto.OfflineExportSelection;
import com.storemanager.domain.system_tool.migration_export.model.MigrationHistoryEntry;
import com.storemanager.domain.system_tool.migration_export.model.MigrationPreviewResult;
import com.storemanager.domain.system_tool.migration_export.presenter.MigrationExportPresenter;
import com.storemanager.core.util.TimeFormatUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MigrationExportController {

    @FXML
    private CheckBox productsCheckBox;

    @FXML
    private CheckBox employeesCheckBox;

    @FXML
    private CheckBox salesCheckBox;

    @FXML
    private CheckBox attendanceCheckBox;

    @FXML
    private CheckBox imagesCheckBox;

    @FXML
    private CheckBox auditLogsCheckBox;

    @FXML
    private Button exportPackageButton;

    @FXML
    private TextField selectedPackageField;

    @FXML
    private Button selectPackageButton;

    @FXML
    private Button previewButton;

    @FXML
    private Button validateButton;

    @FXML
    private Button importButton;

    @FXML
    private Label migrationStatusLabel;

    @FXML
    private Label previewExportVersionLabel;

    @FXML
    private Label previewSourceSystemLabel;

    @FXML
    private Label previewBusinessNameLabel;

    @FXML
    private Label previewRecordCountsLabel;

    @FXML
    private Label previewWarningCountLabel;

    @FXML
    private Label previewErrorCountLabel;

    @FXML
    private TextArea previewWarningsArea;

    @FXML
    private TextArea previewErrorsArea;

    @FXML
    private TableView<MigrationHistoryEntry> historyTable;

    @FXML
    private TableColumn<MigrationHistoryEntry, String> dateColumn;

    @FXML
    private TableColumn<MigrationHistoryEntry, Object> directionColumn;

    @FXML
    private TableColumn<MigrationHistoryEntry, Object> statusColumn;

    @FXML
    private TableColumn<MigrationHistoryEntry, Object> recordCountColumn;

    @FXML
    private TableColumn<MigrationHistoryEntry, Object> userColumn;

    private MigrationExportPresenter presenter;

    private File selectedPackageFile;

    @FXML
    public void initialize() {
        UiFeedback.emptyTable(historyTable, "No records found.");


        presenter =
                new MigrationExportPresenter(
                        this
                );

        dateColumn.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleStringProperty(
                        TimeFormatUtil.formatDateTime(
                                cellData.getValue().getDate()
                        )
                )
        );
        directionColumn.setCellValueFactory(
                new PropertyValueFactory<>("direction")
        );
        statusColumn.setCellValueFactory(
                new PropertyValueFactory<>("status")
        );
        recordCountColumn.setCellValueFactory(
                new PropertyValueFactory<>("recordCount")
        );
        userColumn.setCellValueFactory(
                new PropertyValueFactory<>("user")
        );

        productsCheckBox.setSelected(true);
        employeesCheckBox.setSelected(true);
        salesCheckBox.setSelected(true);
        attendanceCheckBox.setSelected(true);
        imagesCheckBox.setSelected(true);
        auditLogsCheckBox.setSelected(true);

        previewWarningsArea.setEditable(false);
        previewErrorsArea.setEditable(false);
        selectedPackageField.setEditable(false);

        presenter.initialize();
    }

    @FXML
    public void onExportPackage() {

        FileChooser fileChooser =
                new FileChooser();

        fileChooser.setTitle("Save business export package");
        fileChooser.setInitialFileName("business-export.zip");
        fileChooser
                .getExtensionFilters()
                .add(
                        new FileChooser.ExtensionFilter(
                                "ZIP files",
                                "*.zip"
                        )
                );

        File outputFile =
                fileChooser.showSaveDialog(
                        exportPackageButton
                                .getScene()
                                .getWindow()
                );

        if (outputFile == null) {
            return;
        }

        presenter.exportPackage(
                outputFile,
                buildSelection()
        );
    }

    @FXML
    public void onSelectPackage() {

        FileChooser fileChooser =
                new FileChooser();

        fileChooser.setTitle("Choose business package");
        fileChooser
                .getExtensionFilters()
                .add(
                        new FileChooser.ExtensionFilter(
                                "ZIP files",
                                "*.zip"
                        )
                );

        File file =
                fileChooser.showOpenDialog(
                        selectPackageButton
                                .getScene()
                                .getWindow()
                );

        if (file == null) {
            return;
        }

        selectedPackageFile = file;
        selectedPackageField.setText(
                file.getAbsolutePath()
        );
        presenter.onPackageSelected(file);
    }

    @FXML
    public void onPreviewPackage() {

        if (selectedPackageFile == null) {
            showError("Select a package file first");
            return;
        }

        presenter.previewPackage(selectedPackageFile);
    }

    @FXML
    public void onValidatePackage() {

        presenter.validateCurrentPackage();
    }

    @FXML
    public void onImportPackage() {

        presenter.importPackage();
    }

    public OfflineExportSelection buildSelection() {

        return new OfflineExportSelection(
                productsCheckBox.isSelected(),
                employeesCheckBox.isSelected(),
                salesCheckBox.isSelected(),
                attendanceCheckBox.isSelected(),
                imagesCheckBox.isSelected(),
                auditLogsCheckBox.isSelected()
        );
    }

    public void setBusy(
            boolean busy
    ) {

        exportPackageButton.setDisable(busy);
        selectPackageButton.setDisable(busy);
        previewButton.setDisable(busy);
        validateButton.setDisable(busy);
        importButton.setDisable(busy);
        productsCheckBox.setDisable(busy);
        employeesCheckBox.setDisable(busy);
        salesCheckBox.setDisable(busy);
        attendanceCheckBox.setDisable(busy);
        imagesCheckBox.setDisable(busy);
        auditLogsCheckBox.setDisable(busy);
    }

    public void setStatus(
            String status
    ) {

        migrationStatusLabel.setText(status);
    }

    public void setPreview(
            MigrationPreviewResult preview
    ) {

        if (preview == null) {
            previewExportVersionLabel.setText("Export Version: -");
            previewSourceSystemLabel.setText("Source: -");
            previewBusinessNameLabel.setText("Business: -");
            previewRecordCountsLabel.setText("Record Counts: -");
            previewWarningCountLabel.setText("Warnings: 0");
            previewErrorCountLabel.setText("Errors: 0");
            previewWarningsArea.clear();
            previewErrorsArea.clear();
            importButton.setDisable(true);
            return;
        }

        previewExportVersionLabel.setText(
                "Export Version: " + safe(preview.getExportVersion())
        );
        previewSourceSystemLabel.setText(
                "Source: " + safe(preview.getSourceSystem())
        );
        previewBusinessNameLabel.setText(
                "Business: " + safe(preview.getBusinessName())
        );
        previewRecordCountsLabel.setText(
                "Record Counts: " + formatCounts(preview.getRecordCounts())
        );
        previewWarningCountLabel.setText(
                "Warnings: " + preview.getWarnings().size()
        );
        previewErrorCountLabel.setText(
                "Errors: " + preview.getValidationErrors().size()
        );
        previewWarningsArea.setText(
                String.join("\n", preview.getWarnings())
        );
        previewErrorsArea.setText(
                String.join("\n", preview.getValidationErrors())
        );
        importButton.setDisable(
                !preview.getValidationErrors().isEmpty()
        );
    }

    public void setHistory(
            List<MigrationHistoryEntry> history
    ) {

        historyTable.setItems(
                FXCollections.observableArrayList(
                        history == null
                                ? List.of()
                                : history
                )
        );
    }

    public void setStatusLevel(
            String statusLabelText
    ) {

        migrationStatusLabel.setText(statusLabelText);
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

    public void showWarning(
            String message
    ) {

        Alert alert =
                new Alert(
                        Alert.AlertType.WARNING
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

    public void clearSelectedPackage() {

        selectedPackageFile = null;
        selectedPackageField.clear();
    }

    private String safe(
            String value
    ) {

        if (value == null || value.trim().isEmpty()) {
            return "-";
        }

        return value.trim();
    }

    private String formatCounts(
            Map<String, Integer> counts
    ) {

        if (counts == null || counts.isEmpty()) {
            return "-";
        }

        return counts.entrySet()
                .stream()
                .map(entry ->
                        entry.getKey() + ": " + entry.getValue()
                )
                .collect(
                        Collectors.joining(", ")
                );
    }
}
