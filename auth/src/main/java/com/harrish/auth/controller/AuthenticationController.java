package com.harrish.auth.controller;

import com.harrish.auth.dto.*;
import com.harrish.auth.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

import static net.logstash.logback.argument.StructuredArguments.kv;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication API for user registration, login, and token refresh")
class AuthenticationController {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationController.class);

    private final AuthenticationService authenticationService;

    AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account with the provided information"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = RegisterResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or validation error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Email already exists",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/register")
    ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        log.info("POST /api/v1/auth/register", 
                kv("method", "POST"),
                kv("path", "/api/v1/auth/register"),
                kv("email", request.email()));
        
        RegisterResponse response = authenticationService.register(request);
        
        // Build Location header with URI of the created resource (REST best practice - RFC 7231)
        // Points to the user resource that would be accessible after authentication
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/users/{id}")
                .buildAndExpand(response.userId())
                .toUri();
        
        log.info("Registration successful", 
                kv("path", "/api/v1/auth/register"),
                kv("status", 201),
                kv("userId", response.userId()),
                kv("location", location.toString()));
        
        return ResponseEntity.created(location).body(response);
    }

    @Operation(
            summary = "Authenticate a user",
            description = "Authenticates a user with email and password and returns JWT tokens"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful",
                    content = @Content(schema = @Schema(implementation = AuthenticationResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/login")
    ResponseEntity<AuthenticationResponse> authenticate(
            @Valid @RequestBody AuthenticationRequest request
    ) {
        log.info("POST /api/v1/auth/login", 
                kv("method", "POST"),
                kv("path", "/api/v1/auth/login"),
                kv("email", request.email()));
        
        AuthenticationResponse response = authenticationService.authenticate(request);
        
        log.info("Authentication successful", 
                kv("path", "/api/v1/auth/login"),
                kv("status", 200));
        
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Refresh JWT token",
            description = "Refreshes the JWT access token using a valid refresh token"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully",
                    content = @Content(schema = @Schema(implementation = AuthenticationResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/refresh-token")
    ResponseEntity<AuthenticationResponse> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request
    ) {
        log.info("POST /api/v1/auth/refresh-token", 
                kv("method", "POST"),
                kv("path", "/api/v1/auth/refresh-token"));
        
        AuthenticationResponse response = authenticationService.refreshToken(request);
        
        log.info("Token refresh successful", 
                kv("path", "/api/v1/auth/refresh-token"),
                kv("status", 200));
        
        return ResponseEntity.ok(response);
    }
}
