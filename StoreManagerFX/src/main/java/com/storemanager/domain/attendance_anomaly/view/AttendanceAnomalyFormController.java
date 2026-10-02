package com.storemanager.domain.attendance_anomaly.view;

import com.storemanager.domain.attendance.model.AttendanceSession;
import com.storemanager.domain.attendance_anomaly.model.AttendanceAnomaly;
import com.storemanager.core.util.TimeFormatUtil;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AttendanceAnomalyFormController {

    private static final Pattern DURATION_PATTERN =
            Pattern.compile("^(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?$");

    @FXML
    private Label anomalyIdLabel;

    @FXML
    private Label attendanceSessionIdLabel;

    @FXML
    private Label employeeLabel;

    @FXML
    private Label typeLabel;

    @FXML
    private Label severityLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Label createdAtLabel;

    @FXML
    private Label resolvedByLabel;

    @FXML
    private Label resolvedAtLabel;

    @FXML
    private TextArea messageArea;

    @FXML
    private TextArea managerReportArea;

    @FXML
    private CheckBox employeeNotificationCheckBox;

    @FXML
    private TextField checkInField;

    @FXML
    private TextField checkOutField;

    @FXML
    private TextField workedHoursField;

    private boolean managerEditable;

    private boolean adminEditable;

    @FXML
    public void initialize() {

        clear();
    }

    public void showAnomaly(
            AttendanceAnomaly anomaly,
            AttendanceSession session,
            String employeeName
    ) {

        if (anomaly == null) {
            clear();
            return;
        }

        anomalyIdLabel.setText(valueOrDash(anomaly.getId()));
        attendanceSessionIdLabel.setText(
                valueOrDash(anomaly.getAttendanceSessionId())
        );
        employeeLabel.setText(valueOrDash(employeeName));

        typeLabel.setText(valueOrDash(anomaly.getType()));
        severityLabel.setText(valueOrDash(anomaly.getSeverity()));
        statusLabel.setText(valueOrDash(anomaly.getStatus()));
        createdAtLabel.setText(formatDateTime(anomaly.getCreatedAt()));
        resolvedByLabel.setText(valueOrDash(anomaly.getResolvedByUserId()));
        resolvedAtLabel.setText(formatDateTime(anomaly.getResolvedAt()));
        messageArea.setText(valueOrEmpty(anomaly.getMessage()));
        managerReportArea.setText(valueOrEmpty(anomaly.getManagerReportText()));
        employeeNotificationCheckBox.setSelected(anomaly.isEmployeeNotified());

        if (session != null) {
            checkInField.setText(formatDateTime(session.getCheckInTime()));
            checkOutField.setText(formatDateTime(session.getCheckOutTime()));
            workedHoursField.setText(formatWorkedHours(session.getWorkedHours()));
        } else {
            checkInField.clear();
            checkOutField.clear();
            workedHoursField.clear();
        }
    }

    public void clear() {

        anomalyIdLabel.setText("-");
        attendanceSessionIdLabel.setText("-");
        employeeLabel.setText("-");

        typeLabel.setText("-");
        severityLabel.setText("-");
        statusLabel.setText("-");
        createdAtLabel.setText("-");
        resolvedByLabel.setText("-");
        resolvedAtLabel.setText("-");
        messageArea.clear();
        managerReportArea.clear();
        employeeNotificationCheckBox.setSelected(true);
        checkInField.clear();
        checkOutField.clear();
        workedHoursField.clear();
    }

    public String readManagerReportText() {

        return managerReportArea.getText();
    }

    public boolean isEmployeeNotificationEnabled() {

        return employeeNotificationCheckBox.isSelected();
    }

    public AttendanceSession readEditedAttendanceSession() {

        AttendanceSession session = new AttendanceSession();

        String sessionIdText = attendanceSessionIdLabel.getText();
        if (sessionIdText != null && !sessionIdText.isBlank()
                && !"-".equals(sessionIdText)) {
            session.setId(Long.parseLong(sessionIdText.trim()));
        }

        session.setCheckInTime(parseDateTime(checkInField.getText()));
        session.setCheckOutTime(parseDateTime(checkOutField.getText()));
        session.setWorkedHours(parseWorkedHours(workedHoursField.getText()));
        return session;
    }

    public void setManagerEditable(
            boolean editable
    ) {

        managerEditable = editable;
        managerReportArea.setEditable(editable);
        managerReportArea.setDisable(!editable);
    }

    public void setAdminEditable(
            boolean editable
    ) {

        adminEditable = editable;
        checkInField.setEditable(editable);
        checkInField.setDisable(!editable);
        checkOutField.setEditable(editable);
        checkOutField.setDisable(!editable);
        workedHoursField.setEditable(editable);
        workedHoursField.setDisable(!editable);
        employeeNotificationCheckBox.setDisable(!editable);
    }

    public void setEmployeeNotificationVisible(
            boolean visible
    ) {

        employeeNotificationCheckBox.setVisible(visible);
        employeeNotificationCheckBox.setManaged(visible);
    }

    public void setBusy(
            boolean busy
    ) {

        managerReportArea.setDisable(busy || !managerEditable);
        checkInField.setDisable(busy || !adminEditable);
        checkOutField.setDisable(busy || !adminEditable);
        workedHoursField.setDisable(busy || !adminEditable);
        employeeNotificationCheckBox.setDisable(
                busy || !adminEditable
        );
    }

    private String formatDateTime(
            LocalDateTime dateTime
    ) {

        return TimeFormatUtil.formatDateTime(dateTime);
    }

    private String formatWorkedHours(
            BigDecimal workedHours
    ) {

        return TimeFormatUtil.formatDurationHours(workedHours);
    }

    private LocalDateTime parseDateTime(
            String value
    ) {

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return TimeFormatUtil.parseDateTime(value);
    }

    private BigDecimal parseWorkedHours(
            String value
    ) {

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String cleaned = value.trim();
        String normalized = cleaned.toLowerCase(Locale.ROOT);

        if (normalized.contains("h")
                || normalized.contains("m")
                || normalized.contains("s")) {
            return BigDecimal.valueOf(parseDurationSeconds(cleaned))
                    .divide(
                            BigDecimal.valueOf(3600L),
                            2,
                            RoundingMode.HALF_UP
                    );
        }

        if (normalized.contains(".")) {
            return new BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP);
        }

        return BigDecimal.valueOf(Long.parseLong(cleaned))
                .divide(
                        BigDecimal.valueOf(3600L),
                        2,
                        RoundingMode.HALF_UP
                );
    }

    private long parseDurationSeconds(
            String value
    ) {

        String normalized =
                value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");

        Matcher matcher =
                DURATION_PATTERN.matcher(normalized);

        if (!matcher.matches()) {
            throw new RuntimeException(
                    "Worked hours must use seconds or a duration like 8h 15m 20s"
            );
        }

        long hours = parseDurationPart(matcher.group(1));
        long minutes = parseDurationPart(matcher.group(2));
        long seconds = parseDurationPart(matcher.group(3));

        return hours * 3600L + minutes * 60L + seconds;
    }

    private long parseDurationPart(
            String value
    ) {

        if (value == null || value.isBlank()) {
            return 0L;
        }

        return Long.parseLong(value);
    }

    private String valueOrDash(
            Object value
    ) {

        if (value == null) {
            return "-";
        }

        return value.toString();
    }

    private String valueOrEmpty(
            String value
    ) {

        return value == null ? "" : value;
    }
}
