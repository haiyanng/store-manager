package com.storeapi.customer.model;

public record Customer(Long id, String email, String passwordHash, String fullName, String phone, boolean active) {
}
