package com.storemanager.domain.audit.view;

import com.storemanager.core.navigation.SceneManager;
import com.storemanager.core.security.PermissionGuard;
import com.storemanager.core.session.AppSession;
import com.storemanager.domain.audit.model.AuditLogFilter;
import com.storemanager.domain.audit.model.AuditLogViewDto;
import com.storemanager.domain.audit.presenter.AuditLogPresenter;
import com.storemanager.domain.user.model.User;
import com.storemanager.core.util.TimeFormatUtil;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AuditLogController {

    @FXML
    private TextField actionField;

    @FXML
    private TextField entityTypeField;

    @FXML
    private TextField moduleField;

    @FXML
    private ComboBox<String> successComboBox;

    @FXML
    private TextField userIdField;

    @FXML
    private DatePicker fromDatePicker;

    @FXML
    private DatePicker toDatePicker;

    @FXML
    private Button searchButton;

    @FXML
    private Button clearButton;

    @FXML
    private TableView<AuditLogViewDto> auditLogTable;

    @FXML
    private TableColumn<AuditLogViewDto, String> createdAtColumn;

    @FXML
    private TableColumn<AuditLogViewDto, String> usernameColumn;

    @FXML
    private TableColumn<AuditLogViewDto, String> moduleColumn;

    @FXML
    private TableColumn<AuditLogViewDto, String> actionColumn;

    @FXML
    private TableColumn<AuditLogViewDto, String> entityTypeColumn;

    @FXML
    private TableColumn<AuditLogViewDto, Long> entityIdColumn;

    @FXML
    private TableColumn<AuditLogViewDto, Long> branchIdColumn;

    @FXML
    private TableColumn<AuditLogViewDto, String> successColumn;

    @FXML
    private TableColumn<AuditLogViewDto, String> reasonColumn;

    @FXML
    private TableColumn<AuditLogViewDto, String> detailsJsonColumn;

    @FXML
    private TableColumn<AuditLogViewDto, String> detailsColumn;

    @FXML
    private Label statusLabel;

    private AuditLogPresenter presenter;

    @FXML
    public void initialize() {

        User currentUser =
                AppSession.getCurrentUser();

        if (currentUser == null) {
            SceneManager.switchScene("/fxml/auth/login.fxml");
            return;
        }

        if (!PermissionGuard.canViewAuditLogs()) {
            throw new RuntimeException("Audit log access denied");
        }

        presenter =
                new AuditLogPresenter(this);

        successComboBox.setItems(
                FXCollections.observableArrayList(
                        "Any",
                        "Success",
                        "Failure"
                )
        );
        successComboBox.setValue("Any");

        configureTable();
        presenter.initialize();
    }

    @FXML
    public void onSearch() {

        presenter.loadAuditLogs(buildFilter());
    }

    @FXML
    public void onClear() {

        actionField.clear();
        entityTypeField.clear();
        moduleField.clear();
        successComboBox.setValue("Any");
        userIdField.clear();
        fromDatePicker.setValue(null);
        toDatePicker.setValue(null);
        presenter.refresh();
    }

    public AuditLogFilter buildFilter() {

        AuditLogFilter filter =
                new AuditLogFilter();

        filter.setAction(clean(actionField.getText()));
        filter.setEntityType(clean(entityTypeField.getText()));
        filter.setModule(clean(moduleField.getText()));
        filter.setSuccess(parseSuccess(successComboBox.getValue()));
        filter.setUserId(parseLong(userIdField.getText()));
        filter.setFromDate(fromDatePicker.getValue());
        filter.setToDate(toDatePicker.getValue());

        return filter;
    }

    public void setAuditLogs(
            java.util.List<AuditLogViewDto> logs
    ) {

        auditLogTable.setItems(
                FXCollections.observableArrayList(logs)
        );
    }

    public void setBusy(
            boolean busy
    ) {

        searchButton.setDisable(busy);
        clearButton.setDisable(busy);
        auditLogTable.setDisable(busy);
    }

    public void setStatus(
            String status
    ) {

        statusLabel.setText(status);
    }

    public void showError(
            String message
    ) {

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void configureTable() {

        auditLogTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );

        createdAtColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        formatDateTime(cellData.getValue().getCreatedAt())
                )
        );
        usernameColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        cellData.getValue().getActorUsername() == null
                                ? cellData.getValue().getUsername()
                                : cellData.getValue().getActorUsername()
                )
        );
        moduleColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        cellData.getValue().getModule()
                )
        );
        actionColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        cellData.getValue().getAction()
                )
        );
        entityTypeColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        cellData.getValue().getEntityType()
                )
        );
        entityIdColumn.setCellValueFactory(
                cellData -> new SimpleObjectProperty<>(
                        cellData.getValue().getEntityId()
                )
        );
        branchIdColumn.setCellValueFactory(
                cellData -> new SimpleObjectProperty<>(
                        cellData.getValue().getBranchId()
                )
        );
        successColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        Boolean.FALSE.equals(cellData.getValue().getSuccess())
                                ? "Failure"
                                : "Success"
                )
        );
        reasonColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        cellData.getValue().getReason()
                )
        );
        detailsJsonColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        cellData.getValue().getDetailsJson()
                )
        );
        detailsColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(
                        cellData.getValue().getDetails()
                )
        );
    }

    private String clean(
            String value
    ) {

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }

    private Long parseLong(
            String value
    ) {

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            return Long.parseLong(value.trim());
        } catch (Exception e) {
            throw new RuntimeException("User id must be numeric");
        }
    }

    private Boolean parseSuccess(
            String value
    ) {

        if (value == null || "Any".equals(value)) {
            return null;
        }

        return "Success".equals(value);
    }

    private String formatDateTime(
            LocalDateTime dateTime
    ) {

        return TimeFormatUtil.formatDateTime(dateTime);
    }
}
