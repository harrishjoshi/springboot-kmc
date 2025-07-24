package com.harrish.auth.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@ConfigurationProperties(prefix = "application.security.jwt")
@Validated
public class JwtProperties {

    @NotBlank(message = "Secret key cannot be blank")
    private final String secretKey;

    @Positive(message = "Expiration must be positive")
    private final long expiration;

    private final RefreshToken refreshToken;

    public JwtProperties(
            String secretKey,
            long expiration,
            RefreshToken refreshToken) {
        this.secretKey = secretKey;
        this.expiration = expiration;
        this.refreshToken = refreshToken;
    }

    public record RefreshToken(@Positive(message = "Refresh token expiration must be positive") long expiration) {
        public RefreshToken(long expiration) {
            this.expiration = expiration;
        }
    }
}
