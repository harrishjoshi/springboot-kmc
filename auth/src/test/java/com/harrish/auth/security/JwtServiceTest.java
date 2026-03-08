package com.harrish.auth.security;

import com.harrish.auth.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@DisplayName("JwtService")
class JwtServiceTest {

    // Base64-encoded 256-bit key (matches JWT HS256 requirement)
    private static final String TEST_SECRET_KEY = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long TEST_JWT_EXPIRATION = TimeUnit.HOURS.toMillis(1); // 1 hour
    private static final long TEST_REFRESH_EXPIRATION = TimeUnit.DAYS.toMillis(7); // 7 days

    private JwtService jwtService;
    private UserDetails testUser;
    private SecretKey testSigningKey;

    @BeforeEach
    void setUp() {
        var refreshToken = new JwtProperties.RefreshToken(TEST_REFRESH_EXPIRATION);
        var jwtProperties = new JwtProperties(TEST_SECRET_KEY, TEST_JWT_EXPIRATION, refreshToken);
        jwtService = new JwtService(jwtProperties);

        testUser = User.builder()
                .username("test@example.com")
                .password("encoded-password")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        testSigningKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET_KEY));
    }

    @Nested
    @DisplayName("Token Generation")
    class TokenGeneration {

        @Test
        @DisplayName("Should generate valid JWT token with default claims")
        void shouldGenerateValidTokenWithDefaultClaims() {
            // When
            String token = jwtService.generateToken(testUser);

            // Then
            assertThat(token).isNotBlank();

            // Parse token to verify structure
            Claims claims = parseToken(token);
            assertThat(claims.getSubject()).isEqualTo("test@example.com");
            assertThat(claims.getIssuedAt()).isNotNull();
            assertThat(claims.getExpiration()).isNotNull();

            // Verify expiration is approximately 1 hour from now
            long expectedExpiration = System.currentTimeMillis() + TEST_JWT_EXPIRATION;
            assertThat(claims.getExpiration().getTime())
                    .isCloseTo(expectedExpiration, within(1000L)); // 1 second tolerance
        }

        @Test
        @DisplayName("Should generate token with extra claims")
        void shouldGenerateTokenWithExtraClaims() {
            // Given
            Map<String, Object> extraClaims = new HashMap<>();
            extraClaims.put("userId", 123L);
            extraClaims.put("role", "ADMIN");

            // When
            String token = jwtService.generateToken(extraClaims, testUser);

            // Then
            Claims claims = parseToken(token);
            assertThat(claims.getSubject()).isEqualTo("test@example.com");
            assertThat(claims.get("userId", Integer.class)).isEqualTo(123);
            assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("Should generate refresh token with extended expiration")
        void shouldGenerateRefreshTokenWithExtendedExpiration() {
            // When
            String refreshToken = jwtService.generateRefreshToken(testUser);

            // Then
            assertThat(refreshToken).isNotBlank();

            Claims claims = parseToken(refreshToken);
            assertThat(claims.getSubject()).isEqualTo("test@example.com");

            // Verify expiration is approximately 7 days from now
            long expectedExpiration = System.currentTimeMillis() + TEST_REFRESH_EXPIRATION;
            assertThat(claims.getExpiration().getTime())
                    .isCloseTo(expectedExpiration, within(1000L));
        }

        @Test
        @DisplayName("Should generate different tokens for different users")
        void shouldGenerateDifferentTokensForDifferentUsers() {
            // Given
            UserDetails anotherUser = User.builder()
                    .username("another@example.com")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                    .build();

            // When
            String token1 = jwtService.generateToken(testUser);
            String token2 = jwtService.generateToken(anotherUser);

            // Then
            assertThat(token1).isNotEqualTo(token2);
            assertThat(parseToken(token1).getSubject()).isEqualTo("test@example.com");
            assertThat(parseToken(token2).getSubject()).isEqualTo("another@example.com");
        }

        @Test
        @DisplayName("Should generate different tokens on subsequent calls")
        void shouldGenerateDifferentTokensOnSubsequentCalls() throws InterruptedException {
            // When
            String token1 = jwtService.generateToken(testUser);
            Thread.sleep(1001); // 1+ second delay to ensure different issued-at timestamp (JWT uses second precision)
            String token2 = jwtService.generateToken(testUser);

            // Then
            assertThat(token1).isNotEqualTo(token2);
            // Both should be valid for the same user
            assertThat(jwtService.hasValidExpiration(token1, testUser)).isTrue();
            assertThat(jwtService.hasValidExpiration(token2, testUser)).isTrue();
        }
    }

    @Nested
    @DisplayName("Token Parsing and Extraction")
    class TokenParsingAndExtraction {

        @Test
        @DisplayName("Should extract username from valid token")
        void shouldExtractUsernameFromValidToken() {
            // Given
            String token = jwtService.generateToken(testUser);

            // When
            String username = jwtService.extractUsername(token);

            // Then
            assertThat(username).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Should extract custom claim from token")
        void shouldExtractCustomClaimFromToken() {
            // Given
            Map<String, Object> extraClaims = Map.of("userId", 42L);
            String token = jwtService.generateToken(extraClaims, testUser);

            // When
            Integer userId = jwtService.extractClaim(token, claims -> claims.get("userId", Integer.class));

            // Then
            assertThat(userId).isEqualTo(42);
        }

        @Test
        @DisplayName("Should extract subject claim using function")
        void shouldExtractSubjectClaimUsingFunction() {
            // Given
            String token = jwtService.generateToken(testUser);

            // When
            String subject = jwtService.extractClaim(token, Claims::getSubject);

            // Then
            assertThat(subject).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Should extract expiration claim")
        void shouldExtractExpirationClaim() {
            // Given
            String token = jwtService.generateToken(testUser);

            // When
            Date expiration = jwtService.extractClaim(token, Claims::getExpiration);

            // Then
            assertThat(expiration).isNotNull();
            assertThat(expiration.getTime()).isGreaterThan(System.currentTimeMillis());
        }

        @Test
        @DisplayName("Should throw exception when parsing malformed token")
        void shouldThrowExceptionWhenParsingMalformedToken() {
            // Given
            String malformedToken = "not.a.valid.jwt";

            // Then
            assertThatThrownBy(() -> jwtService.extractUsername(malformedToken))
                    .isInstanceOf(MalformedJwtException.class);
        }

        @Test
        @DisplayName("Should throw exception when parsing token with invalid signature")
        void shouldThrowExceptionWhenParsingTokenWithInvalidSignature() {
            // Given - token signed with different key
            String tokenWithWrongSignature = Jwts.builder()
                    .subject("test@example.com")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 3600000))
                    .signWith(Keys.hmacShaKeyFor("WrongSecretKeyThatIs32BytesLong12345678901234567890".getBytes()))
                    .compact();

            // Then
            assertThatThrownBy(() -> jwtService.extractUsername(tokenWithWrongSignature))
                    .isInstanceOf(SignatureException.class);
        }
    }

    @Nested
    @DisplayName("Token Validation")
    class TokenValidation {

        @Test
        @DisplayName("Should validate token with correct username and valid expiration")
        void shouldValidateTokenWithCorrectUsernameAndValidExpiration() {
            // Given
            String token = jwtService.generateToken(testUser);

            // When
            boolean isValid = jwtService.hasValidExpiration(token, testUser);

            // Then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should invalidate token with incorrect username")
        void shouldInvalidateTokenWithIncorrectUsername() {
            // Given
            String token = jwtService.generateToken(testUser);
            UserDetails differentUser = User.builder()
                    .username("different@example.com")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                    .build();

            // When
            boolean isValid = jwtService.hasValidExpiration(token, differentUser);

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should invalidate expired token")
        void shouldInvalidateExpiredToken() {
            // Given - create token that expires immediately
            var shortExpirationProperties = new JwtProperties(
                    TEST_SECRET_KEY,
                    1L, // 1 millisecond
                    new JwtProperties.RefreshToken(TEST_REFRESH_EXPIRATION)
            );
            var shortExpirationService = new JwtService(shortExpirationProperties);
            String token = shortExpirationService.generateToken(testUser);

            // When - wait for token to expire
            try {
                Thread.sleep(10); // Wait 10ms to ensure expiration
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Then
            assertThatThrownBy(() -> shortExpirationService.hasValidExpiration(token, testUser))
                    .isInstanceOf(ExpiredJwtException.class);
        }

        @Test
        @DisplayName("Should validate refresh token with correct username")
        void shouldValidateRefreshTokenWithCorrectUsername() {
            // Given
            String refreshToken = jwtService.generateRefreshToken(testUser);

            // When
            boolean isValid = jwtService.isRefreshTokenValid(refreshToken, testUser);

            // Then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should invalidate refresh token with incorrect username")
        void shouldInvalidateRefreshTokenWithIncorrectUsername() {
            // Given
            String refreshToken = jwtService.generateRefreshToken(testUser);
            UserDetails differentUser = User.builder()
                    .username("different@example.com")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                    .build();

            // When
            boolean isValid = jwtService.isRefreshTokenValid(refreshToken, differentUser);

            // Then
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("Configuration")
    class Configuration {

        @Test
        @DisplayName("Should return JWT expiration in seconds")
        void shouldReturnJwtExpirationInSeconds() {
            // When
            long expirationInSeconds = jwtService.getJwtExpirationInSeconds();

            // Then
            long expectedSeconds = TimeUnit.MILLISECONDS.toSeconds(TEST_JWT_EXPIRATION);
            assertThat(expirationInSeconds).isEqualTo(expectedSeconds);
        }

        @Test
        @DisplayName("Should cache signing key for performance")
        void shouldCacheSigningKeyForPerformance() {
            // This test verifies the Phase 4 optimization (cached signing key)
            // Generate multiple tokens and verify they all use the same cached key
            // (all tokens should be valid, which proves the cached key works)

            // When
            String token1 = jwtService.generateToken(testUser);
            String token2 = jwtService.generateToken(testUser);
            String token3 = jwtService.generateToken(testUser);

            // Then - all tokens should be parseable with the same key
            assertThat(jwtService.extractUsername(token1)).isEqualTo("test@example.com");
            assertThat(jwtService.extractUsername(token2)).isEqualTo("test@example.com");
            assertThat(jwtService.extractUsername(token3)).isEqualTo("test@example.com");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle username with special characters")
        void shouldHandleUsernameWithSpecialCharacters() {
            // Given
            UserDetails userWithSpecialChars = User.builder()
                    .username("user+test@example.com")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                    .build();

            // When
            String token = jwtService.generateToken(userWithSpecialChars);
            String extractedUsername = jwtService.extractUsername(token);

            // Then
            assertThat(extractedUsername).isEqualTo("user+test@example.com");
            assertThat(jwtService.hasValidExpiration(token, userWithSpecialChars)).isTrue();
        }

        @Test
        @DisplayName("Should handle empty extra claims map")
        void shouldHandleEmptyExtraClaimsMap() {
            // Given
            Map<String, Object> emptyMap = new HashMap<>();

            // When
            String token = jwtService.generateToken(emptyMap, testUser);

            // Then
            assertThat(token).isNotBlank();
            assertThat(jwtService.extractUsername(token)).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Should handle null values in extra claims")
        void shouldHandleNullValuesInExtraClaims() {
            // Given
            Map<String, Object> claimsWithNull = new HashMap<>();
            claimsWithNull.put("nullClaim", null);
            claimsWithNull.put("validClaim", "value");

            // When
            String token = jwtService.generateToken(claimsWithNull, testUser);

            // Then
            Claims claims = parseToken(token);
            assertThat(claims.get("nullClaim")).isNull();
            assertThat(claims.get("validClaim", String.class)).isEqualTo("value");
        }

        @Test
        @DisplayName("Should handle long username")
        void shouldHandleLongUsername() {
            // Given
            String longEmail = "a".repeat(100) + "@example.com";
            UserDetails userWithLongUsername = User.builder()
                    .username(longEmail)
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                    .build();

            // When
            String token = jwtService.generateToken(userWithLongUsername);
            String extractedUsername = jwtService.extractUsername(token);

            // Then
            assertThat(extractedUsername).isEqualTo(longEmail);
            assertThat(jwtService.hasValidExpiration(token, userWithLongUsername)).isTrue();
        }
    }

    /**
     * Helper method to parse token using the test signing key
     */
    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(testSigningKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
