package com.customershopfx.auth.service;

import com.customershopfx.app.AppContext;
import com.customershopfx.auth.model.AuthResponse;
import com.customershopfx.auth.model.Customer;
import com.customershopfx.common.api.ApiClient;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Map;

public class AuthApiService {
    private final ApiClient api = AppContext.apiClient();

    public AuthResponse login(String email, String password) {
        return api.post("/api/auth/login", Map.of("email", email, "password", password), new TypeReference<>() {});
    }

    public AuthResponse register(String email, String password, String fullName, String phone) {
        return api.post("/api/auth/register",
                Map.of("email", email, "password", password, "fullName", fullName, "phone", phone == null ? "" : phone),
                new TypeReference<>() {});
    }

    public Customer me() {
        return api.get("/api/auth/me", new TypeReference<>() {});
    }
}
