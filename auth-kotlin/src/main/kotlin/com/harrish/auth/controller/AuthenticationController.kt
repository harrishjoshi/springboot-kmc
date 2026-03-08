package com.harrish.auth.controller

import com.harrish.auth.dto.AuthenticationRequest
import com.harrish.auth.dto.AuthenticationResponse
import com.harrish.auth.dto.RegisterRequest
import com.harrish.auth.dto.RegisterResponse
import com.harrish.auth.dto.TokenRefreshRequest
import com.harrish.auth.service.AuthenticationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication API for user registration, login, and token refresh")
class AuthenticationController(
    private val authenticationService: AuthenticationService
) {
    @Operation(
        summary = "Register a new user",
        description = "Creates a new user account with the provided information"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "User registered successfully",
            content = [Content(schema = Schema(implementation = RegisterResponse::class))]),
        ApiResponse(responseCode = "400", description = "Invalid input or email already exists",
            content = [Content()])
    ])
    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<RegisterResponse> =
        ResponseEntity.ok(authenticationService.register(request))

    @Operation(
        summary = "Authenticate a user",
        description = "Authenticates a user with email and password and returns JWT tokens"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Authentication successful",
            content = [Content(schema = Schema(implementation = AuthenticationResponse::class))]),
        ApiResponse(responseCode = "401", description = "Invalid credentials",
            content = [Content()])
    ])
    @PostMapping("/login")
    fun authenticateUser(@Valid @RequestBody request: AuthenticationRequest): ResponseEntity<AuthenticationResponse> =
        ResponseEntity.ok(authenticationService.authenticateUser(request))

    @Operation(
        summary = "Refresh JWT token",
        description = "Refreshes the JWT access token using a valid refresh token"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Token refreshed successfully",
            content = [Content(schema = Schema(implementation = AuthenticationResponse::class))]),
        ApiResponse(responseCode = "401", description = "Invalid or expired refresh token",
            content = [Content()])
    ])
    @PostMapping("/refresh-token")
    fun refreshToken(@Valid @RequestBody request: TokenRefreshRequest): ResponseEntity<AuthenticationResponse> =
        ResponseEntity.ok(authenticationService.refreshToken(request))
}
