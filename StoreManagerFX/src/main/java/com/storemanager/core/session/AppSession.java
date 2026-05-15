package com.storemanager.core.session;

public class AppSession {

    private static String currentUsername;

    public static void setCurrentUsername(
            String username
    ) {

        currentUsername = username;
    }

    public static String getCurrentUsername() {

        return currentUsername;
    }
}