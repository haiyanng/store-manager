package com.storeapi.setup.presenter;

import com.storeapi.setup.StoreApiSetupApplication;
import com.storeapi.setup.model.DatabaseConfig;
import com.storeapi.setup.service.DatabaseConfigFileService;
import com.storeapi.setup.service.DatabaseConnectionTestService;
import com.storeapi.setup.viewmodel.DatabaseSetupViewModel;
import javafx.concurrent.Task;

public class DatabaseSetupPresenter {
    private final DatabaseSetupViewModel viewModel;
    private final DatabaseConfigFileService configFileService;
    private final DatabaseConnectionTestService connectionTestService;

    public DatabaseSetupPresenter(DatabaseSetupViewModel viewModel,
                                  DatabaseConfigFileService configFileService,
                                  DatabaseConnectionTestService connectionTestService) {
        this.viewModel = viewModel;
        this.configFileService = configFileService;
        this.connectionTestService = connectionTestService;
    }

    public void initialize() {
        try {
            if (!configFileService.exists()) {
                configFileService.createSampleConfig();
                viewModel.apply(configFileService.defaultConfig());
                viewModel.statusMessage("Created sample config file at " + configFileService.configPath());
                return;
            }

            DatabaseConfig config = configFileService.read();
            viewModel.apply(config);
            if (config.password() == null || config.password().isBlank()) {
                viewModel.statusMessage("Config loaded. Password is optional.");
            } else {
                viewModel.statusMessage("Config loaded from external file.");
            }
        } catch (Exception ex) {
            viewModel.resetToDefaults();
            viewModel.statusMessage("Unable to load external config. Check the file and try again.");
        }
    }

    public void testConnection() {
        if (viewModel.busy()) {
            return;
        }
        if (!validateInputs()) {
            return;
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String plainPassword = resolvePasswordForTest();
                DatabaseConfig config = buildConfig(plainPassword);
                if (!connectionTestService.testConnection(config, plainPassword)) {
                    throw new IllegalStateException("Connection test failed.");
                }
                return null;
            }
        };

        task.setOnRunning(event -> {
            viewModel.busy(true);
            viewModel.statusMessage("Testing database connection...");
        });
        task.setOnSucceeded(event -> {
            viewModel.busy(false);
            viewModel.statusMessage("Connection OK. You can start API.");
        });
        task.setOnFailed(event -> {
            viewModel.busy(false);
            viewModel.statusMessage("Database connection failed: " + rootMessage(task.getException()));
        });

        Thread thread = new Thread(task, "db-connection-test");
        thread.setDaemon(true);
        thread.start();
    }

    public void startApi() {
        if (viewModel.busy()) {
            return;
        }
        if (!validateInputs()) {
            return;
        }

        Task<DatabaseConfig> task = new Task<>() {
            @Override
            protected DatabaseConfig call() throws Exception {
                String password = resolvePasswordForStart();
                DatabaseConfig config = buildConfig(password);
                if (!connectionTestService.testConnection(config, password)) {
                    throw new IllegalStateException("Connection test failed.");
                }
                configFileService.save(config);
                return config;
            }
        };

        task.setOnRunning(event -> {
            viewModel.busy(true);
            viewModel.statusMessage("Starting API...");
        });
        task.setOnSucceeded(event -> {
            viewModel.busy(false);
            StoreApiSetupApplication.requestStartApi(task.getValue());
        });
        task.setOnFailed(event -> {
            viewModel.busy(false);
            viewModel.statusMessage("Connection failed: " + rootMessage(task.getException()));
        });

        Thread thread = new Thread(task, "db-start-api");
        thread.setDaemon(true);
        thread.start();
    }

    public void saveConfig() {
        if (!validateInputs()) {
            return;
        }
        try {
            String password = resolvePasswordForSave();
            DatabaseConfig config = buildConfig(password);
            configFileService.save(config);
            viewModel.loadedPassword(password);
            viewModel.passwordProperty().set("");
            viewModel.statusMessage("Config saved.");
        } catch (Exception ex) {
            viewModel.statusMessage("Cannot save config: " + rootMessage(ex));
        }
    }

    private boolean validateInputs() {
        if (isBlank(viewModel.hostProperty().get()) ||
                isBlank(viewModel.portProperty().get()) ||
                isBlank(viewModel.databaseNameProperty().get()) ||
                isBlank(viewModel.usernameProperty().get()) ||
                isBlank(viewModel.serverPortProperty().get())) {
            viewModel.statusMessage("Please fill in all database fields.");
            return false;
        }
        try {
            Integer.parseInt(viewModel.portProperty().get().trim());
            Integer.parseInt(viewModel.serverPortProperty().get().trim());
        } catch (NumberFormatException ex) {
            viewModel.statusMessage("Port fields must be numeric.");
            return false;
        }
        return true;
    }

    private DatabaseConfig buildConfig(String password) {
        return new DatabaseConfig(
                viewModel.hostProperty().get().trim(),
                Integer.parseInt(viewModel.portProperty().get().trim()),
                viewModel.databaseNameProperty().get().trim(),
                viewModel.usernameProperty().get().trim(),
                password == null ? "" : password,
                Integer.parseInt(viewModel.serverPortProperty().get().trim())
        );
    }

    private String resolvePasswordForTest() {
        String password = viewModel.passwordProperty().get();
        if (password != null && !password.isBlank()) {
            return password;
        }
        if (viewModel.loadedPassword() != null) {
            return viewModel.loadedPassword();
        }
        return "";
    }

    private String resolvePasswordForSave() {
        String password = viewModel.passwordProperty().get();
        if (password != null && !password.isBlank()) {
            return password;
        }
        return "";
    }

    private String resolvePasswordForStart() {
        String password = viewModel.passwordProperty().get();
        if (password != null && !password.isBlank()) {
            return password;
        }
        if (viewModel.loadedPassword() != null && !viewModel.loadedPassword().isBlank()) {
            return viewModel.loadedPassword();
        }
        return "";
    }

    private String rootMessage(Throwable throwable) {
        if (throwable == null) {
            return "Unknown error";
        }
        Throwable cursor = throwable;
        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        return cursor.getMessage() == null ? cursor.getClass().getSimpleName() : cursor.getMessage();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
