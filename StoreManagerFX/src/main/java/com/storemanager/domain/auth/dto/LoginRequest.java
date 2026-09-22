package com.storemanager.domain.auth.dto;

public record LoginRequest(

        String username,

        String password

) {
}