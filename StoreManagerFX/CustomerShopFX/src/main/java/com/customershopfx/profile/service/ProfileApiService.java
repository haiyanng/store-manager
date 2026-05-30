package com.customershopfx.profile.service;

import com.customershopfx.app.AppContext;
import com.customershopfx.common.api.ApiClient;
import com.customershopfx.profile.model.CustomerProfile;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Map;

public class ProfileApiService {
    private final ApiClient api = AppContext.apiClient();

    public CustomerProfile profile() {
        return api.get("/api/customers/profile", new TypeReference<>() {});
    }

    public CustomerProfile updateProfile(String fullName, String phone) {
        return api.put("/api/customers/profile", Map.of("fullName", fullName, "phone", phone == null ? "" : phone),
                new TypeReference<>() {});
    }

    public void changePassword(String currentPassword, String newPassword) {
        api.put("/api/customers/password", Map.of("currentPassword", currentPassword, "newPassword", newPassword),
                new TypeReference<Void>() {});
    }
}
