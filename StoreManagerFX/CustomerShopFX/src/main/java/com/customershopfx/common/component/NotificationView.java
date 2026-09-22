package com.customershopfx.common.component;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.stage.Window;
import org.controlsfx.control.Notifications;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.util.Duration;

public class NotificationView extends StackPane {
    public NotificationView() {
        setMouseTransparent(true);
        setVisible(false);
        setManaged(false);
    }

    public void showSuccess(String title, String message) {
        show(title, message, true, Duration.seconds(2.8));
    }

    public void showError(String title, String message) {
        show(title, message, false, Duration.seconds(4.0));
    }

    public void show(String title, String message, boolean success, Duration visibleFor) {
        if (Platform.isFxApplicationThread()) {
            showOnFxThread(title, message, success, visibleFor);
        } else {
            Platform.runLater(() -> showOnFxThread(title, message, success, visibleFor));
        }
    }

    private void showOnFxThread(String title, String message, boolean success, Duration visibleFor) {
        FontIcon icon = new FontIcon(success ? "fas-check-circle" : "fas-circle-exclamation");
        icon.getStyleClass().add(success ? "text-success" : "text-error");
        Notifications notifications = Notifications.create()
                .title(title)
                .text(message)
                .graphic(icon)
                .position(Pos.CENTER)
                .hideAfter(visibleFor)
                .hideCloseButton()
                .styleClass("white-toast", success ? "toast-success" : "toast-error");
        Window owner = getScene() == null ? null : getScene().getWindow();
        if (owner != null) {
            notifications.owner(owner);
        }
        notifications.show();
    }
}
