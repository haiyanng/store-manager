package com.customershopfx.app;

import com.customershopfx.auth.model.Customer;

public final class SessionManager {
    private static String token;
    private static Customer customer;

    private SessionManager() {
    }

    public static String token() {
        return token;
    }

    public static Customer customer() {
        return customer;
    }

    public static boolean authenticated() {
        return token != null && !token.isBlank();
    }

    public static void start(String jwt, Customer currentCustomer) {
        token = jwt;
        customer = currentCustomer;
    }

    public static void updateCustomer(Customer currentCustomer) {
        customer = currentCustomer;
    }

    public static void clear() {
        token = null;
        customer = null;
    }
}
