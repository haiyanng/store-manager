package com.storeapi.customer.service;

import com.storeapi.customer.dto.CustomerDto;
import com.storeapi.customer.dto.PasswordChangeRequest;
import com.storeapi.customer.dto.ProfileUpdateRequest;
import com.storeapi.customer.model.Customer;
import com.storeapi.customer.repository.CustomerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {
    private final CustomerRepository customers;
    private final PasswordEncoder passwordEncoder;

    public CustomerService(CustomerRepository customers, PasswordEncoder passwordEncoder) {
        this.customers = customers;
        this.passwordEncoder = passwordEncoder;
    }

    public CustomerDto profile(Long id) {
        return dto(customers.findById(id).orElseThrow());
    }

    public CustomerDto updateProfile(Long id, ProfileUpdateRequest request) {
        customers.updateProfile(id, request.fullName(), request.phone());
        return profile(id);
    }

    public void changePassword(Long id, PasswordChangeRequest request) {
        Customer customer = customers.findById(id).orElseThrow();
        if (!passwordEncoder.matches(request.currentPassword(), customer.passwordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        customers.updatePassword(id, passwordEncoder.encode(request.newPassword()));
    }

    private CustomerDto dto(Customer customer) {
        return new CustomerDto(customer.id(), customer.email(), customer.fullName(), customer.phone());
    }
}
