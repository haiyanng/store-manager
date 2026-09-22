package com.storeapi.setup.service;

import com.storeapi.setup.model.DatabaseConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatabaseConfigFileService {
    private static final Pattern JDBC_URL = Pattern.compile("^jdbc:mysql://([^:/?]+):(\\d+)/([^?]+)(?:\\?.*)?$");
    private final SetupPathService paths;

    public DatabaseConfigFileService() {
        this(new SetupPathService());
    }

    public DatabaseConfigFileService(SetupPathService paths) {
        this.paths = paths;
    }

    public boolean exists() {
        return Files.exists(paths.configFile());
    }

    public Path configPath() {
        return paths.configFile();
    }

    public DatabaseConfig defaultConfig() {
        return new DatabaseConfig("localhost", 3306, "store_manager_db", "root", "", 8080);
    }

    public void createSampleConfig() throws IOException {
        save(defaultConfig());
    }

    public DatabaseConfig read() throws IOException {
        Properties properties = new Properties();
        try (var input = Files.newInputStream(paths.configFile())) {
            properties.load(input);
        }

        String url = required(properties, "spring.datasource.url");
        Matcher matcher = JDBC_URL.matcher(url.trim());
        if (!matcher.matches()) {
            throw new IllegalStateException("Invalid JDBC URL: " + url);
        }

        String host = matcher.group(1);
        int port = Integer.parseInt(matcher.group(2));
        String databaseName = matcher.group(3);
        String username = properties.getProperty("spring.datasource.username", "root").trim();
        String encryptedPassword = properties.getProperty("spring.datasource.password.encrypted", "").trim();
        String plainPassword = properties.getProperty("spring.datasource.password", "").trim();
        int serverPort = Integer.parseInt(properties.getProperty("server.port", "8080").trim());
        String password = "";
        if (!encryptedPassword.isBlank()) {
            password = new DatabasePasswordCryptoService(new LocalSecretKeyService(paths)).decrypt(encryptedPassword);
        } else if (!plainPassword.isBlank()) {
            password = plainPassword;
        }
        return new DatabaseConfig(host, port, databaseName, username, password, serverPort);
    }

    public void save(DatabaseConfig config) throws IOException {
        Files.createDirectories(paths.configDir());
        new LocalSecretKeyService(paths).loadOrCreateKey();
        StringBuilder builder = new StringBuilder();
        builder.append("server.port=").append(config.serverPort()).append('\n');
        builder.append("spring.datasource.url=").append(config.jdbcUrl()).append('\n');
        builder.append("spring.datasource.username=").append(config.username()).append('\n');
        String password = config.password() == null ? "" : config.password().trim();
        if (password.isBlank()) {
            builder.append("spring.datasource.password=").append('\n');
        } else {
            String encrypted = new DatabasePasswordCryptoService(new LocalSecretKeyService(paths)).encrypt(password);
            builder.append("spring.datasource.password.encrypted=").append(encrypted).append('\n');
        }
        Files.writeString(paths.configFile(), builder.toString(), StandardCharsets.UTF_8);
    }

    private String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing property: " + key);
        }
        return value;
    }
}
