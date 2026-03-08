package com.harrish.auth.security;

import com.harrish.auth.config.JwtProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * Validates JWT configuration at application startup to ensure security requirements are met.
 */
@Component
public class JwtSecurityValidator {

    private static final Logger log = LoggerFactory.getLogger(JwtSecurityValidator.class);
    private static final int MINIMUM_SECRET_LENGTH_BYTES = 32; // 256 bits
    private static final String DEFAULT_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    private final JwtProperties jwtProperties;

    public JwtSecurityValidator(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    public void validateJwtConfiguration() {
        validateSecretKey();
        validateExpirationTimes();
        logSecurityConfiguration();
    }

    private void validateSecretKey() {
        String secretKey = jwtProperties.getSecretKey();

        // Check if default secret is being used
        if (DEFAULT_SECRET.equals(secretKey)) {
            log.warn("⚠️  WARNING: Using default JWT secret key! This is INSECURE for production.");
            log.warn("⚠️  Set JWT_SECRET_KEY environment variable with a strong secret.");

            // In production, fail fast
            String profile = System.getProperty("spring.profiles.active", "");
            if (profile.contains("prod")) {
                throw new IllegalStateException(
                        "Cannot start application with default JWT secret in production! " +
                                "Set JWT_SECRET_KEY environment variable."
                );
            }
        }

        // Validate secret length
        try {
            byte[] decodedKey = Base64.getDecoder().decode(secretKey);
            if (decodedKey.length < MINIMUM_SECRET_LENGTH_BYTES) {
                throw new IllegalStateException(
                        String.format("JWT secret key must be at least %d bytes (256 bits). Current: %d bytes",
                                MINIMUM_SECRET_LENGTH_BYTES, decodedKey.length)
                );
            }
            log.info("✓ JWT secret key validation passed ({} bytes)", decodedKey.length);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("JWT secret key must be valid Base64 encoded", e);
        }
    }

    private void validateExpirationTimes() {
        long accessTokenExpiration = jwtProperties.getExpiration();
        long refreshTokenExpiration = jwtProperties.getRefreshToken().expiration();

        // Access token should be short-lived (recommended: 15-60 minutes)
        if (accessTokenExpiration > 3600000) { // 1 hour
            log.warn("⚠️  Access token expiration is longer than recommended ({}ms). Consider reducing to 15-60 minutes.",
                    accessTokenExpiration);
        }

        // Refresh token should be longer than access token
        if (refreshTokenExpiration <= accessTokenExpiration) {
            throw new IllegalStateException(
                    "Refresh token expiration must be longer than access token expiration"
            );
        }

        log.info("✓ JWT expiration times validated");
        log.info("  - Access token: {}ms ({}min)", accessTokenExpiration, accessTokenExpiration / 60000);
        log.info("  - Refresh token: {}ms ({}h)", refreshTokenExpiration, refreshTokenExpiration / 3600000);
    }

    private void logSecurityConfiguration() {
        log.info("=".repeat(60));
        log.info("JWT Security Configuration:");
        log.info("  Secret: {}", isDefaultSecret() ? "DEFAULT (⚠️  INSECURE)" : "CUSTOM (✓)");
        log.info("  Access Token TTL: {} minutes", jwtProperties.getExpiration() / 60000);
        log.info("  Refresh Token TTL: {} hours", jwtProperties.getRefreshToken().expiration() / 3600000);
        log.info("=".repeat(60));
    }

    private boolean isDefaultSecret() {
        return DEFAULT_SECRET.equals(jwtProperties.getSecretKey());
    }
}
