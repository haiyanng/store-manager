package com.storeapi.customer.controller;

import com.storeapi.customer.dto.PasswordChangeRequest;
import com.storeapi.customer.service.CustomerService;
import com.storeapi.security.CustomerPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerPasswordController {
    private final CustomerService customers;

    public CustomerPasswordController(CustomerService customers) {
        this.customers = customers;
    }

    @PutMapping("/password")
    void changePassword(@AuthenticationPrincipal CustomerPrincipal principal, @Valid @RequestBody PasswordChangeRequest request) {
        customers.changePassword(principal.id(), request);
    }
}
