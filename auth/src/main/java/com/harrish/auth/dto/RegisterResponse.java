package com.harrish.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RegisterResponse(
        @JsonProperty("user_id")
        Long userId,
        
        @JsonProperty("message")
        String message
) {
    // Compact constructor to set default values
    public RegisterResponse {
        if (message == null) {
            message = "User registered successfully";
        }
    }
}