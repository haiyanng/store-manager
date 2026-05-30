package com.storeapi.auth.controller;

import com.storeapi.auth.dto.AuthResponse;
import com.storeapi.auth.dto.LoginRequest;
import com.storeapi.auth.dto.RegisterRequest;
import com.storeapi.customer.dto.CustomerDto;
import com.storeapi.security.CustomerPrincipal;
import com.storeapi.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    CustomerDto me(@AuthenticationPrincipal CustomerPrincipal principal) {
        return authService.me(principal.id());
    }
}
