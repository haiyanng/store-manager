package com.storeapi.auth.service;

import com.storeapi.auth.dto.AuthResponse;
import com.storeapi.auth.dto.LoginRequest;
import com.storeapi.auth.dto.RegisterRequest;
import com.storeapi.customer.dto.CustomerDto;
import com.storeapi.customer.model.Customer;
import com.storeapi.customer.repository.CustomerRepository;
import com.storeapi.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final CustomerRepository customers;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(CustomerRepository customers, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.customers = customers;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        customers.findByEmail(request.email()).ifPresent(c -> {
            throw new IllegalArgumentException("Email is already registered");
        });
        Long id = customers.create(request.email(), passwordEncoder.encode(request.password()), request.fullName(), request.phone());
        Customer customer = customers.findById(id).orElseThrow();
        return response(customer);
    }

    public AuthResponse login(LoginRequest request) {
        Customer customer = customers.findByEmail(request.email())
                .filter(Customer::active)
                .filter(c -> passwordEncoder.matches(request.password(), c.passwordHash()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        return response(customer);
    }

    public CustomerDto me(Long id) {
        return customers.findById(id).map(this::dto).orElseThrow();
    }

    private AuthResponse response(Customer customer) {
        return new AuthResponse(jwtService.generateToken(customer.id(), customer.email()), dto(customer));
    }

    private CustomerDto dto(Customer customer) {
        return new CustomerDto(customer.id(), customer.email(), customer.fullName(), customer.phone());
    }
}
