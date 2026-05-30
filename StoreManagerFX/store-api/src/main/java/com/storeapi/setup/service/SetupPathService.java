package com.storeapi.setup.service;

import java.nio.file.Path;
import java.util.logging.Logger;

public class SetupPathService {
    public Path userDir() {
        return Path.of(System.getProperty("user.dir"));
    }

    public Path configDir() {
        return userDir().resolve("config");
    }

    public Path configFile() {
        return configDir().resolve("store-api.properties");
    }

    public Path keyFile() {
        return configDir().resolve(".store-api.key");
    }

    public void logSnapshot(Logger log) {
        log.info("Working directory: " + userDir().toAbsolutePath().normalize());
        log.info("Config directory: " + configDir().toAbsolutePath().normalize());
        log.info("Config file: " + configFile().toAbsolutePath().normalize());
        log.info("Key file: " + keyFile().toAbsolutePath().normalize());
        log.info("Config exists: " + java.nio.file.Files.exists(configFile()));
        log.info("Key exists: " + java.nio.file.Files.exists(keyFile()));
    }
}
