package com.customershopfx.app;

import com.customershopfx.common.api.ApiClient;

public final class AppContext {
    private static final ApiClient API_CLIENT = new ApiClient();

    private AppContext() {
    }

    public static ApiClient apiClient() {
        return API_CLIENT;
    }
}
