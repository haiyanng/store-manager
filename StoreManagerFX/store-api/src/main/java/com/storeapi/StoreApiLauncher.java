package com.storeapi;

import com.storeapi.setup.StoreApiSetupApplication;
import com.storeapi.setup.model.DatabaseConfig;
import com.storeapi.setup.service.SetupPathService;
import org.springframework.boot.SpringApplication;

import java.util.logging.Logger;

public final class StoreApiLauncher {
    private static final Logger log = Logger.getLogger(StoreApiLauncher.class.getName());

    private StoreApiLauncher() {
    }

    public static void main(String[] args) {
        SetupPathService paths = new SetupPathService();

        try {
            paths.logSnapshot(log);
            log.info("Opening database setup UI");
            StoreApiSetupApplication.launchSetup(args);

            DatabaseConfig approvedConfig = StoreApiSetupApplication.consumeApprovedConfig();
            if (approvedConfig == null) {
                log.info("Setup closed without API start. Exiting launcher.");
                return;
            }

            applyRuntimeConfig(approvedConfig);
            log.info("Starting Spring Boot API");
            SpringApplication.run(StoreApiApplication.class, args);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void applyRuntimeConfig(DatabaseConfig config) {
        System.setProperty("spring.datasource.url", config.jdbcUrl());
        System.setProperty("spring.datasource.username", config.username());
        System.setProperty("spring.datasource.password", config.password() == null ? "" : config.password());
        System.setProperty("server.port", Integer.toString(config.serverPort()));
    }
}
