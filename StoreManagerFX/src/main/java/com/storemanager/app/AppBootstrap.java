package com.storemanager.app;

import com.storemanager.config.ConfigService;
import com.storemanager.config.DatabaseSettings;
import com.storemanager.core.database.ConnectionFactory;
import com.storemanager.core.database.DatabaseInitializer;
import com.storemanager.core.navigation.SceneManager;
import javafx.stage.Stage;

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

        try {

            if (ConfigService.exists()) {

                DatabaseSettings settings =
                        ConfigService
                                .loadDatabaseSettings()
                                .orElseThrow();

                boolean connected =
                        ConnectionFactory.testConnection(settings);

                if (connected) {

                    ConnectionFactory.setSettings(settings);

                    DatabaseInitializer.initialize();

                    SceneManager.switchScene(
                            "/fxml/auth/login.fxml"
                    );

                    return;
                }
            }

            SceneManager.switchScene(
                    "/fxml/database_setup/database-setup.fxml"
            );

        } catch (Exception e) {

            e.printStackTrace();

            SceneManager.switchScene(
                    "/fxml/database_setup/database-setup.fxml"
            );
        }
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
