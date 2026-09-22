package com.storemanager.domain.database_setup.service;

import com.storemanager.config.ConfigService;
import com.storemanager.config.DatabaseSettings;
import com.storemanager.core.database.ConnectionFactory;

public class DatabaseSetupService {

    public boolean testConnection(
            DatabaseSettings settings
    ) {

        return ConnectionFactory.testConnection(settings);
    }

    public boolean saveSettings(
            DatabaseSettings settings
    ) {

        return ConfigService.saveDatabaseSettings(settings);
    }
}