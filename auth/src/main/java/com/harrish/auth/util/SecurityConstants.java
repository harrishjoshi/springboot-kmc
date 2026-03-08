package com.harrish.auth.util;

/**
 * Security-related constants used across the authentication service.
 * Centralizes security configuration values for consistency.
 */
public final class SecurityConstants {

    private SecurityConstants() {
        // Prevent instantiation
    }

    // JWT Token constants
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String BEARER_TOKEN_TYPE = "Bearer";
    public static final int BEARER_PREFIX_LENGTH = BEARER_PREFIX.length(); // 7

    // Security timeouts (in seconds)
    public static final long CORS_MAX_AGE_SECONDS = 3600L; // 1 hour
    public static final long HSTS_MAX_AGE_SECONDS = 31536000L; // 1 year (365 days)
}
