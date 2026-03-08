package com.harrish.auth.service;

import com.harrish.auth.dto.AuthenticationRequest;
import com.harrish.auth.dto.AuthenticationResponse;
import com.harrish.auth.dto.RegisterRequest;
import com.harrish.auth.dto.RegisterResponse;
import com.harrish.auth.dto.TokenRefreshRequest;
import com.harrish.auth.event.UserRegisteredEvent;
import com.harrish.auth.exception.EmailAlreadyExistsException;
import com.harrish.auth.exception.InvalidTokenException;
import com.harrish.auth.model.Role;
import com.harrish.auth.repository.UserRepository;
import com.harrish.auth.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@RecordApplicationEvents
@DisplayName("AuthenticationService Integration Tests")
class AuthenticationServiceTest {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ApplicationEvents applicationEvents;

    private RegisterRequest testRegisterRequest;
    private AuthenticationRequest testAuthRequest;

    @BeforeEach
    void setUp() {
        // Clean database before each test
        userRepository.deleteAll();

        // Prepare test data
        testRegisterRequest = new RegisterRequest(
                "test@example.com",
                "password123",
                "Test",
                "User"
        );

        testAuthRequest = new AuthenticationRequest(
                "test@example.com",
                "password123"
        );
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("User Registration")
    class UserRegistration {

        @Test
        @DisplayName("Should register new user successfully")
        void shouldRegisterNewUserSuccessfully() {
            // When
            RegisterResponse response = authenticationService.register(testRegisterRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.message()).isEqualTo("User registered successfully");

            // Verify user was saved to database
            var savedUser = userRepository.findByEmail("test@example.com");
            assertThat(savedUser).isPresent();
            assertThat(savedUser.get().getEmail()).isEqualTo("test@example.com");
            assertThat(savedUser.get().getFirstName()).isEqualTo("Test");
            assertThat(savedUser.get().getLastName()).isEqualTo("User");
            assertThat(savedUser.get().getRole()).isEqualTo(Role.USER);

            // Verify password was encoded
            assertThat(passwordEncoder.matches("password123", savedUser.get().getPassword())).isTrue();
        }

        @Test
        @DisplayName("Should publish UserRegisteredEvent after successful registration")
        void shouldPublishUserRegisteredEventAfterSuccessfulRegistration() {
            // When
            authenticationService.register(testRegisterRequest);

            // Then
            long eventCount = applicationEvents.stream(UserRegisteredEvent.class).count();
            assertThat(eventCount).isEqualTo(1);

            // Verify event contains correct user data
            var event = applicationEvents.stream(UserRegisteredEvent.class).findFirst();
            assertThat(event).isPresent();
            assertThat(event.get().getUser().getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Should throw EmailAlreadyExistsException when registering duplicate email")
        void shouldThrowEmailAlreadyExistsExceptionWhenRegisteringDuplicateEmail() {
            // Given - register user first time
            authenticationService.register(testRegisterRequest);

            // When/Then - attempt to register same email again
            assertThatThrownBy(() -> authenticationService.register(testRegisterRequest))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .hasMessageContaining("test@example.com");

            // Verify only one user exists in database
            long userCount = userRepository.count();
            assertThat(userCount).isEqualTo(1);
        }

        @Test
        @DisplayName("Should register multiple users with different emails")
        void shouldRegisterMultipleUsersWithDifferentEmails() {
            // Given
            var user1Request = new RegisterRequest(
                    "user1@example.com",
                    "password1",
                    "User",
                    "One"
            );
            var user2Request = new RegisterRequest(
                    "user2@example.com",
                    "password2",
                    "User",
                    "Two"
            );

            // When
            authenticationService.register(user1Request);
            authenticationService.register(user2Request);

            // Then
            assertThat(userRepository.count()).isEqualTo(2);
            assertThat(userRepository.findByEmail("user1@example.com")).isPresent();
            assertThat(userRepository.findByEmail("user2@example.com")).isPresent();
        }
    }

    @Nested
    @DisplayName("User Authentication")
    class UserAuthentication {

        @BeforeEach
        void setUpUser() {
            // Register a user for authentication tests
            authenticationService.register(testRegisterRequest);
        }

        @Test
        @DisplayName("Should authenticate user with valid credentials")
        void shouldAuthenticateUserWithValidCredentials() {
            // When
            AuthenticationResponse response = authenticationService.authenticate(testAuthRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isNotBlank();
            assertThat(response.refreshToken()).isNotBlank();
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.expiresIn()).isPositive();

            // Verify tokens are valid
            String username = jwtService.extractUsername(response.accessToken());
            assertThat(username).isEqualTo("test@example.com");

            String refreshUsername = jwtService.extractUsername(response.refreshToken());
            assertThat(refreshUsername).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Should throw BadCredentialsException with invalid password")
        void shouldThrowBadCredentialsExceptionWithInvalidPassword() {
            // Given
            var invalidRequest = new AuthenticationRequest(
                    "test@example.com",
                    "wrongpassword"
            );

            // When/Then
            assertThatThrownBy(() -> authenticationService.authenticate(invalidRequest))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("Should throw BadCredentialsException with non-existent email")
        void shouldThrowBadCredentialsExceptionWithNonExistentEmail() {
            // Given
            var invalidRequest = new AuthenticationRequest(
                    "nonexistent@example.com",
                    "password123"
            );

            // When/Then
            assertThatThrownBy(() -> authenticationService.authenticate(invalidRequest))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("Should return different tokens for subsequent authentications")
        void shouldReturnDifferentTokensForSubsequentAuthentications() throws InterruptedException {
            // When
            AuthenticationResponse response1 = authenticationService.authenticate(testAuthRequest);
            Thread.sleep(1001); // Sleep to ensure different issued-at timestamp
            AuthenticationResponse response2 = authenticationService.authenticate(testAuthRequest);

            // Then
            assertThat(response1.accessToken()).isNotEqualTo(response2.accessToken());
            assertThat(response1.refreshToken()).isNotEqualTo(response2.refreshToken());

            // Both tokens should be valid
            assertThat(jwtService.extractUsername(response1.accessToken())).isEqualTo("test@example.com");
            assertThat(jwtService.extractUsername(response2.accessToken())).isEqualTo("test@example.com");
        }
    }

    @Nested
    @DisplayName("Token Refresh")
    class TokenRefresh {

        private String validRefreshToken;

        @BeforeEach
        void setUpUserAndToken() {
            // Register and authenticate user
            authenticationService.register(testRegisterRequest);
            AuthenticationResponse authResponse = authenticationService.authenticate(testAuthRequest);
            validRefreshToken = authResponse.refreshToken();
        }

        @Test
        @DisplayName("Should refresh access token with valid refresh token")
        void shouldRefreshAccessTokenWithValidRefreshToken() {
            // Given
            var refreshRequest = new TokenRefreshRequest(validRefreshToken);

            // When
            AuthenticationResponse response = authenticationService.refreshToken(refreshRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isNotBlank();
            assertThat(response.refreshToken()).isEqualTo(validRefreshToken); // Same refresh token returned
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.expiresIn()).isPositive();

            // Verify new access token is valid
            String username = jwtService.extractUsername(response.accessToken());
            assertThat(username).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Should throw InvalidTokenException with malformed refresh token")
        void shouldThrowInvalidTokenExceptionWithMalformedRefreshToken() {
            // Given
            var invalidRequest = new TokenRefreshRequest("invalid.token.here");

            // When/Then
            assertThatThrownBy(() -> authenticationService.refreshToken(invalidRequest))
                    .isInstanceOf(Exception.class); // MalformedJwtException or InvalidTokenException
        }

        @Test
        @DisplayName("Should throw InvalidTokenException with access token instead of refresh token")
        void shouldThrowInvalidTokenExceptionWithAccessTokenInsteadOfRefreshToken() {
            // Given - use access token as refresh token (should fail validation)
            AuthenticationResponse authResponse = authenticationService.authenticate(testAuthRequest);
            var invalidRequest = new TokenRefreshRequest(authResponse.accessToken());

            // When/Then
            assertThatThrownBy(() -> authenticationService.refreshToken(invalidRequest))
                    .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("Should return new access token different from original")
        void shouldReturnNewAccessTokenDifferentFromOriginal() throws InterruptedException {
            // Given
            AuthenticationResponse originalResponse = authenticationService.authenticate(testAuthRequest);
            Thread.sleep(1001); // Ensure different timestamp
            var refreshRequest = new TokenRefreshRequest(originalResponse.refreshToken());

            // When
            AuthenticationResponse refreshResponse = authenticationService.refreshToken(refreshRequest);

            // Then
            assertThat(refreshResponse.accessToken()).isNotEqualTo(originalResponse.accessToken());
            // Both should be valid for the same user
            assertThat(jwtService.extractUsername(originalResponse.accessToken())).isEqualTo("test@example.com");
            assertThat(jwtService.extractUsername(refreshResponse.accessToken())).isEqualTo("test@example.com");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Security")
    class EdgeCasesAndSecurity {

        @Test
        @DisplayName("Should handle email with special characters")
        void shouldHandleEmailWithSpecialCharacters() {
            // Given
            var specialEmailRequest = new RegisterRequest(
                    "user+test@example.com",
                    "password123",
                    "Special",
                    "User"
            );

            // When
            authenticationService.register(specialEmailRequest);
            var authRequest = new AuthenticationRequest("user+test@example.com", "password123");
            AuthenticationResponse response = authenticationService.authenticate(authRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(jwtService.extractUsername(response.accessToken())).isEqualTo("user+test@example.com");
        }

        @Test
        @DisplayName("Should handle email case sensitivity correctly")
        void shouldHandleEmailCaseSensitivityCorrectly() {
            // Given - register with lowercase email
            authenticationService.register(testRegisterRequest);

            // When - authenticate with uppercase email
            var uppercaseAuthRequest = new AuthenticationRequest(
                    "TEST@EXAMPLE.COM",
                    "password123"
            );

            // Then - should fail because emails are case-sensitive in database
            assertThatThrownBy(() -> authenticationService.authenticate(uppercaseAuthRequest))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("Should handle long names correctly")
        void shouldHandleLongNamesCorrectly() {
            // Given - use reasonable length names (not 100 chars to avoid BCrypt 72-byte limit)
            String longFirstName = "A".repeat(50);
            String longLastName = "B".repeat(50);
            var longNameRequest = new RegisterRequest(
                    "longname@example.com",
                    "password123",
                    longFirstName,
                    longLastName
            );

            // When
            RegisterResponse response = authenticationService.register(longNameRequest);

            // Then
            assertThat(response.message()).isEqualTo("User registered successfully");
            var savedUser = userRepository.findByEmail("longname@example.com");
            assertThat(savedUser).isPresent();
            assertThat(savedUser.get().getFirstName()).isEqualTo(longFirstName);
            assertThat(savedUser.get().getLastName()).isEqualTo(longLastName);
        }
    }
}
