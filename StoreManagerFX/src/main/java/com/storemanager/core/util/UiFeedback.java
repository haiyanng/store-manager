package com.storemanager.core.util;

import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Shared presentation rules; never changes persisted dates or monetary values. */
public final class UiFeedback {
    private static final String EMPTY_TEXT = UiFeedback.class.getName() + ".empty";
    private UiFeedback() { }

    public static String money(BigDecimal amount) {
        return MoneyFormatUtil.format(amount);
    }

    public static <T> void moneyColumn(TableColumn<T, BigDecimal> column) {
        column.setCellFactory(ignored -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : money(value));
                setAlignment(Pos.CENTER_RIGHT);
            }
        });
    }

    public static <T> void booleanColumn(TableColumn<T, Boolean> column, String yes, String no) {
        column.setCellFactory(ignored -> new TableCell<>() {
            @Override protected void updateItem(Boolean value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : value ? yes : no);
            }
        });
    }

    public static void emptyTable(TableView<?> table, String message) {
        table.getProperties().put(EMPTY_TEXT, message);
        table.setPlaceholder(new Label(message));
    }

    public static void status(Label label, String message, TableView<?>... tables) {
        message = message == null ? "" : message.trim();
        boolean idle = message.isEmpty() || message.equals("Ready");
        label.setText(message.startsWith("Ready. ") ? message.substring(7) : message);
        label.setVisible(!idle);
        label.setManaged(!idle);
        boolean loading = message.startsWith("Loading ");
        boolean failed = message.startsWith("Unable to load ") || message.startsWith("Cannot load ");
        label.setStyle(failed ? "-fx-text-fill: #b42318;" : "");
        for (TableView<?> table : tables) {
            if (loading) {
                ProgressIndicator progress = new ProgressIndicator();
                progress.setMaxSize(28, 28);
                VBox placeholder = new VBox(8, progress, new Label(message));
                placeholder.setAlignment(Pos.CENTER);
                table.setPlaceholder(placeholder);
            } else if (failed) {
                // A stale list must not masquerade as fresh data after a failed refresh.
                table.getItems().clear();
                Label error = new Label(message + ".\nTry loading the list again.");
                error.setWrapText(true);
                error.setStyle("-fx-text-fill: #b42318;");
                table.setPlaceholder(error);
            } else if (message.equals("Ready") || message.startsWith("Ready.") || message.endsWith(" records loaded")) {
                table.setPlaceholder(new Label((String) table.getProperties().getOrDefault(EMPTY_TEXT, "No records found.")));
            }
        }
    }

    public static void datePicker(DatePicker picker) {
        picker.setPromptText("yyyy-MM-dd");
        picker.setConverter(new StringConverter<>() {
            @Override public String toString(LocalDate value) {
                return value == null ? "" : DateTimeFormatter.ISO_LOCAL_DATE.format(value);
            }
            @Override public LocalDate fromString(String value) {
                return value == null || value.isBlank() ? null : LocalDate.parse(value.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
            }
        });
    }

    public static LocalDate readDate(DatePicker picker) {
        String text = picker.getEditor().getText();
        try {
            LocalDate value = text == null || text.isBlank() ? null
                    : LocalDate.parse(text.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
            picker.setValue(value);
            return value;
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("Enter a valid date in yyyy-MM-dd format", e);
        }
    }

    public static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Store Manager");
        alert.setHeaderText("Action could not be completed");
        String text = message == null || message.isBlank() ? "Please try again." : message;
        alert.setContentText(text.endsWith(".") ? text : text + ".");
        alert.showAndWait();
    }

    public static boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Store Manager");
        alert.setHeaderText(title);
        alert.setContentText(message);
        return alert.showAndWait().filter(javafx.scene.control.ButtonType.OK::equals).isPresent();
    }
}
