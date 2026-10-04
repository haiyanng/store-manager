package com.storemanager.app;

import com.storemanager.config.ConfigService;
import com.storemanager.config.DatabaseSettings;
import com.storemanager.core.database.ConnectionFactory;
import com.storemanager.core.database.DatabaseInitializer;
import com.storemanager.core.navigation.SceneManager;
import com.storemanager.domain.license.mock.LicenseServiceMock;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import com.storemanager.core.runtime.async.AsyncTaskRunner;

public class AppBootstrap {

    private static final double DEFAULT_WIDTH =
            1400;

    private static final double DEFAULT_HEIGHT =
            900;

    private static final double MIN_WIDTH =
            1200;

    private static final double MIN_HEIGHT =
            750;

    public static void start(
            Stage stage
    ) {

        configureStage(stage);

        SceneManager.initialize(stage);

        VBox loading = new VBox(18, new ProgressIndicator(), new Label("Connecting to Store Manager..."));
        loading.setAlignment(Pos.CENTER);
        loading.setStyle("-fx-font-size: 16px; -fx-background-color: #f7f7f4;");
        stage.setScene(new Scene(loading));
        stage.show();
        AsyncTaskRunner.run(() -> {
            if (!ConfigService.exists()) return false;
            DatabaseSettings settings = ConfigService.loadDatabaseSettings().orElseThrow();
            if (!ConnectionFactory.testConnection(settings)) return false;
            ConnectionFactory.setSettings(settings);
            DatabaseInitializer.initialize();
            try { new LicenseServiceMock().refreshOnStartup(); }
            catch (RuntimeException error) { error.printStackTrace(); }
            return true;
        }, connected -> SceneManager.switchScene(connected
                ? "/fxml/auth/login.fxml" : "/fxml/database_setup/database-setup.fxml"), error -> {
            error.printStackTrace();
            SceneManager.switchScene("/fxml/database_setup/database-setup.fxml");
            com.storemanager.core.util.UiFeedback.showError(error.getMessage());
        }, null);
    }

    private static void configureStage(
            Stage stage
    ) {

        stage.setWidth(DEFAULT_WIDTH);
        stage.setHeight(DEFAULT_HEIGHT);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
    }
}
