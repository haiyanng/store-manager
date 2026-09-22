package com.storeapi.setup;

import com.storeapi.setup.model.DatabaseConfig;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class StoreApiSetupApplication extends Application {
    private static final AtomicBoolean startRequested = new AtomicBoolean(false);
    private static final AtomicReference<DatabaseConfig> approvedConfig = new AtomicReference<>();

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(StoreApiSetupApplication.class.getResource("/com/storeapi/setup/fxml/database-setup.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 760, 520);
        scene.getStylesheets().add(StoreApiSetupApplication.class.getResource("/com/storeapi/setup/css/setup.css").toExternalForm());

        stage.setTitle("Store API Database Setup");
        stage.setScene(scene);
        stage.setMinWidth(760);
        stage.setMinHeight(520);
        stage.show();
    }

    public static void launchSetup(String[] args) {
        resetLaunchState();
        launch(args);
    }

    public static void requestStartApi(DatabaseConfig config) {
        Platform.runLater(() -> {
            approvedConfig.set(config);
            startRequested.set(true);
            Platform.exit();
        });
    }

    public static DatabaseConfig consumeApprovedConfig() {
        if (!startRequested.getAndSet(false)) {
            approvedConfig.set(null);
            return null;
        }
        return approvedConfig.getAndSet(null);
    }

    private static void resetLaunchState() {
        startRequested.set(false);
        approvedConfig.set(null);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
