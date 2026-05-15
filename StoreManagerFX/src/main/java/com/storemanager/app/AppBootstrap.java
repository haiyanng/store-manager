package com.storemanager.app;

import com.storemanager.config.ConfigService;
import com.storemanager.config.DatabaseSettings;
import com.storemanager.core.database.ConnectionFactory;
import com.storemanager.core.database.DatabaseInitializer;
import com.storemanager.core.navigation.SceneManager;
import javafx.stage.Stage;

public class AppBootstrap {

    public static void start(
            Stage stage
    ) {

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
}