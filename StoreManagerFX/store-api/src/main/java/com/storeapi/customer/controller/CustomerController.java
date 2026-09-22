package com.storeapi.customer.controller;

import com.storeapi.customer.dto.CustomerDto;
import com.storeapi.customer.dto.ProfileUpdateRequest;
import com.storeapi.customer.service.CustomerService;
import com.storeapi.security.CustomerPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers/profile")
public class CustomerController {
    private final CustomerService customers;

    public CustomerController(CustomerService customers) {
        this.customers = customers;
    }

    @GetMapping
    CustomerDto profile(@AuthenticationPrincipal CustomerPrincipal principal) {
        return customers.profile(principal.id());
    }

    @PutMapping
    CustomerDto update(@AuthenticationPrincipal CustomerPrincipal principal, @Valid @RequestBody ProfileUpdateRequest request) {
        return customers.updateProfile(principal.id(), request);
    }
}
