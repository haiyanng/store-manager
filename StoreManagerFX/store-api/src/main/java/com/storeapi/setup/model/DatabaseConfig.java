package com.storeapi.setup.model;

public record DatabaseConfig(
        String host,
        int port,
        String databaseName,
        String username,
        String password,
        int serverPort
) {
    public String jdbcUrl() {
        return "jdbc:mysql://%s:%d/%s?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh"
                .formatted(host, port, databaseName);
    }
}
