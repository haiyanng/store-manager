package com.storeapi.auth.dto;

import com.storeapi.customer.dto.CustomerDto;

public record AuthResponse(String token, CustomerDto customer) {
}
