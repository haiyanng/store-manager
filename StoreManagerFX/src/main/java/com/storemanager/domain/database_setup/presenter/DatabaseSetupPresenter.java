package com.storemanager.domain.database_setup.presenter;

import com.storemanager.config.DatabaseSettings;
import com.storemanager.core.database.ConnectionFactory;
import com.storemanager.core.database.DatabaseInitializer;
import com.storemanager.core.navigation.SceneManager;
import com.storemanager.domain.database_setup.service.DatabaseSetupService;

public class DatabaseSetupPresenter {

    private final DatabaseSetupService service =
            new DatabaseSetupService();

    public boolean testConnection(
            DatabaseSettings settings
    ) {

        return service.testConnection(settings);
    }

    public boolean saveAndContinue(
            DatabaseSettings settings
    ) {

        boolean connected =
                service.testConnection(settings);

        if (!connected) {
            return false;
        }

        boolean saved =
                service.saveSettings(settings);

        if (!saved) {
            return false;
        }

        ConnectionFactory.setSettings(settings);

        DatabaseInitializer.initialize();

        SceneManager.switchScene(
                "/fxml/auth/login.fxml"
        );

        return true;
    }
}