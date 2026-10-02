package com.storemanager.core.session;

import com.storemanager.domain.user.model.User;

public class AppSession {

    private static User currentUser;

    public static void setCurrentUser(
            User user
    ) {

        currentUser = user;
    }

    public static User getCurrentUser() {

        return currentUser;
    }

    public static boolean isLoggedIn() {

        return currentUser != null;
    }

    public static void clear() {

        currentUser = null;

    }
}
