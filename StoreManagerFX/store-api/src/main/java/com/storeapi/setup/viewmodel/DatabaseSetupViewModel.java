package com.storeapi.setup.viewmodel;

import com.storeapi.setup.model.DatabaseConfig;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class DatabaseSetupViewModel {
    private final StringProperty host = new SimpleStringProperty("localhost");
    private final StringProperty port = new SimpleStringProperty("3306");
    private final StringProperty databaseName = new SimpleStringProperty("store_manager_db");
    private final StringProperty username = new SimpleStringProperty("root");
    private final StringProperty password = new SimpleStringProperty();
    private final StringProperty serverPort = new SimpleStringProperty("8080");
    private final StringProperty statusMessage = new SimpleStringProperty("Configure the database connection.");
    private final BooleanProperty busy = new SimpleBooleanProperty(false);
    private String loadedPassword = "";

    public void resetToDefaults() {
        host.set("localhost");
        port.set("3306");
        databaseName.set("store_manager_db");
        username.set("root");
        password.set("");
        serverPort.set("8080");
        statusMessage.set("Configure the database connection.");
        busy.set(false);
        loadedPassword = "";
    }

    public void apply(DatabaseConfig config) {
        host.set(config.host());
        port.set(Integer.toString(config.port()));
        databaseName.set(config.databaseName());
        username.set(config.username());
        password.set("");
        serverPort.set(Integer.toString(config.serverPort()));
        loadedPassword = config.password() == null ? "" : config.password();
    }

    public DatabaseConfig toConfig(String passwordValue) {
        return new DatabaseConfig(
                host.get().trim(),
                Integer.parseInt(port.get().trim()),
                databaseName.get().trim(),
                username.get().trim(),
                passwordValue == null ? "" : passwordValue,
                Integer.parseInt(serverPort.get().trim())
        );
    }

    public String hostPropertyValue() {
        return host.get();
    }

    public StringProperty hostProperty() {
        return host;
    }

    public StringProperty portProperty() {
        return port;
    }

    public StringProperty databaseNameProperty() {
        return databaseName;
    }

    public StringProperty usernameProperty() {
        return username;
    }

    public StringProperty passwordProperty() {
        return password;
    }

    public StringProperty serverPortProperty() {
        return serverPort;
    }

    public StringProperty statusMessageProperty() {
        return statusMessage;
    }

    public BooleanProperty busyProperty() {
        return busy;
    }

    public String loadedPassword() {
        return loadedPassword;
    }

    public void loadedPassword(String loadedPassword) {
        this.loadedPassword = loadedPassword == null ? "" : loadedPassword;
    }

    public void statusMessage(String message) {
        statusMessage.set(message);
    }

    public String statusMessage() {
        return statusMessage.get();
    }

    public void busy(boolean value) {
        busy.set(value);
    }

    public boolean busy() {
        return busy.get();
    }
}
