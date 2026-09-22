package com.storemanager.core.session;

import com.storemanager.domain.user.model.User;

public class AppSession {

    private static User currentUser;

    private static Long activeBranchId;

    private static String activeBranchName;

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

    public static Long getActiveBranchId() {

        return activeBranchId;
    }

    public static void setActiveBranchId(
            Long activeBranchId
    ) {

        AppSession.activeBranchId = activeBranchId;
    }

    public static String getActiveBranchName() {

        return activeBranchName;
    }

    public static void setActiveBranchName(
            String activeBranchName
    ) {

        AppSession.activeBranchName = activeBranchName;
    }

    public static void setActiveBranch(
            Long branchId,
            String branchName
    ) {

        AppSession.activeBranchId = branchId;
        AppSession.activeBranchName = branchName;
    }

    public static void clear() {

        currentUser = null;
        activeBranchId = null;
        activeBranchName = null;
    }
}
