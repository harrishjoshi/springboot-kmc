package com.harrish.auth.service;

import com.harrish.auth.dto.*;
import com.harrish.auth.exception.EmailAlreadyExistsException;
import com.harrish.auth.exception.InvalidTokenException;
import com.harrish.auth.exception.UserNotFoundException;
import com.harrish.auth.model.Role;
import com.harrish.auth.model.User;
import com.harrish.auth.repository.UserRepository;
import com.harrish.auth.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        // Create user
        var user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();

        // Save user
        userRepository.save(user);

        // Return response with success message
        return new RegisterResponse("User registered successfully");
    }

    @Transactional(readOnly = true)
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        // Authenticate user
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        // Get user
        var user = userRepository.findByEmail(request.email())
                .orElseThrow(UserNotFoundException::new);

        // Generate tokens
        var accessToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

        // Get JWT expiration time in seconds from application properties
        long expiresIn = jwtService.getJwtExpirationInSeconds();

        // Return response
        return new AuthenticationResponse(accessToken, refreshToken, "Bearer", expiresIn);
    }

    public AuthenticationResponse refreshToken(TokenRefreshRequest request) {
        // Extract username from refresh token
        var refreshToken = request.refreshToken();
        var userEmail = jwtService.extractUsername(refreshToken);

        if (userEmail != null) {
            // Get user
            UserDetails userDetails = userRepository.findByEmail(userEmail)
                    .orElseThrow(UserNotFoundException::new);

            // Validate refresh token
            if (jwtService.isRefreshTokenValid(refreshToken, userDetails)) {
                // Generate new access token
                var accessToken = jwtService.generateToken(userDetails);

                // Get JWT expiration time in seconds from application properties
                long expiresIn = jwtService.getJwtExpirationInSeconds();

                // Return response
                return new AuthenticationResponse(accessToken, refreshToken, "Bearer", expiresIn);
            }
        }

        throw new InvalidTokenException();
    }
}
