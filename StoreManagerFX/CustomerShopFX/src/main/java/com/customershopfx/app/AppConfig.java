package com.customershopfx.app;

public final class AppConfig {
    public static final String API_BASE_URL = System.getProperty("store.api.url", "http://localhost:8080");

    private AppConfig() {
    }
}
