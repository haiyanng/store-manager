package com.storemanager.config;

public class DatabaseSettings {

    private String host;
    private int port;
    private String databaseName;
    private String username;
    private String password;

    public DatabaseSettings() {
    }

    public DatabaseSettings(
            String host,
            int port,
            String databaseName,
            String username,
            String password
    ) {
        this.host = host;
        this.port = port;
        this.databaseName = databaseName;
        this.username = username;
        this.password = password;
    }

    public String buildDatabaseUrl() {
        return "jdbc:mysql://"
                + host
                + ":"
                + port
                + "/"
                + databaseName
                + "?createDatabaseIfNotExist=true"
                + "&useUnicode=true"
                + "&characterEncoding=UTF-8"
                + "&serverTimezone=Asia/Ho_Chi_Minh";
    }

    public String buildServerUrl() {
        return "jdbc:mysql://"
                + host
                + ":"
                + port
                + "/"
                + "?useUnicode=true"
                + "&characterEncoding=UTF-8"
                + "&serverTimezone=Asia/Ho_Chi_Minh";
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}