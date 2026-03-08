package com.harrish.auth.service;

import com.harrish.auth.dto.*;
import com.harrish.auth.event.UserRegisteredEvent;
import com.harrish.auth.exception.EmailAlreadyExistsException;
import com.harrish.auth.exception.InvalidTokenException;
import com.harrish.auth.exception.UserNotFoundException;
import com.harrish.auth.model.User;
import com.harrish.auth.repository.UserRepository;
import com.harrish.auth.security.JwtService;
import com.harrish.auth.security.UserPrincipal;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final UserFactory userFactory;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final ApplicationEventPublisher eventPublisher;

    public AuthenticationService(
            UserRepository userRepository,
            UserFactory userFactory,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.userFactory = userFactory;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        // Create user using factory
        User user = userFactory.createStandardUser(request);

        // Save user
        user = userRepository.save(user);

        // Publish user registration event (Observer pattern)
        eventPublisher.publishEvent(new UserRegisteredEvent(this, user));

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

        // Get user and wrap in UserPrincipal
        var user = userRepository.findByEmail(request.email())
                .orElseThrow(UserNotFoundException::new);
        var userPrincipal = new UserPrincipal(user);

        // Generate tokens
        var accessToken = jwtService.generateToken(userPrincipal);
        var refreshToken = jwtService.generateRefreshToken(userPrincipal);

        // Get JWT expiration time in seconds from application properties
        long expiresIn = jwtService.getJwtExpirationInSeconds();

        // Return response
        return new AuthenticationResponse(accessToken, refreshToken, "Bearer", expiresIn);
    }

    @Transactional(readOnly = true)
    public AuthenticationResponse refreshToken(TokenRefreshRequest request) {
        // Extract username from refresh token
        var refreshToken = request.refreshToken();
        var userEmail = jwtService.extractUsername(refreshToken);

        // Check for null or blank email
        if (userEmail == null || userEmail.isBlank()) {
            throw new InvalidTokenException();
        }
        
        // Get user and wrap in UserPrincipal
        var user = userRepository.findByEmail(userEmail)
                .orElseThrow(UserNotFoundException::new);
        UserDetails userDetails = new UserPrincipal(user);

        // Validate refresh token
        if (jwtService.isRefreshTokenValid(refreshToken, userDetails)) {
            // Generate new access token
            var accessToken = jwtService.generateToken(userDetails);

            // Get JWT expiration time in seconds from application properties
            long expiresIn = jwtService.getJwtExpirationInSeconds();

            // Return response
            return new AuthenticationResponse(accessToken, refreshToken, "Bearer", expiresIn);
        }

        throw new InvalidTokenException();
    }
}
