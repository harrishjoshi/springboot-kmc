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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

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
        long startTime = System.currentTimeMillis();
        log.info("User registration started", kv("email", request.email()), kv("step", "start"));
        
        // Check if user already exists
        if (userRepository.existsByEmail(request.email())) {
            log.warn("Registration failed - email already exists", kv("email", request.email()));
            throw new EmailAlreadyExistsException(request.email());
        }

        // Create user using factory
        User user = userFactory.createStandardUser(request);
        log.debug("User entity created", kv("email", request.email()), kv("step", "user_created"));

        // Save user
        user = userRepository.save(user);
        long saveTime = System.currentTimeMillis();
        log.debug("User saved to database", 
                kv("userId", user.getId()), 
                kv("email", user.getEmail()), 
                kv("step", "user_saved"),
                kv("duration_ms", saveTime - startTime));

        // Publish user registration event (Observer pattern)
        eventPublisher.publishEvent(new UserRegisteredEvent(this, user));
        log.debug("User registration event published", 
                kv("userId", user.getId()), 
                kv("step", "event_published"));

        long totalTime = System.currentTimeMillis();
        log.info("User registration completed successfully", 
                kv("userId", user.getId()), 
                kv("email", user.getEmail()),
                kv("step", "complete"),
                kv("duration_ms", totalTime - startTime));

        // Return response with success message
        return new RegisterResponse("User registered successfully");
    }

    @Transactional(readOnly = true)
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("User authentication started", kv("email", request.email()), kv("step", "start"));
        
        // Authenticate user
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );
        long authTime = System.currentTimeMillis();
        log.debug("Authentication manager validated credentials", 
                kv("email", request.email()),
                kv("step", "auth_validated"),
                kv("duration_ms", authTime - startTime));

        // Get user and wrap in UserPrincipal
        var user = userRepository.findByEmail(request.email())
                .orElseThrow(UserNotFoundException::new);
        var userPrincipal = new UserPrincipal(user);
        
        // Add userId to MDC for all subsequent logs in this request
        MDC.put("userId", String.valueOf(user.getId()));
        log.debug("User principal created", 
                kv("userId", user.getId()),
                kv("step", "principal_created"));

        // Generate tokens
        var accessToken = jwtService.generateToken(userPrincipal);
        var refreshToken = jwtService.generateRefreshToken(userPrincipal);
        long tokenTime = System.currentTimeMillis();
        log.debug("JWT tokens generated", 
                kv("userId", user.getId()),
                kv("step", "tokens_generated"),
                kv("duration_ms", tokenTime - authTime));

        // Get JWT expiration time in seconds from application properties
        long expiresIn = jwtService.getJwtExpirationInSeconds();

        long totalTime = System.currentTimeMillis();
        log.info("User authentication completed successfully", 
                kv("userId", user.getId()),
                kv("email", user.getEmail()),
                kv("step", "complete"),
                kv("duration_ms", totalTime - startTime));

        // Return response
        return new AuthenticationResponse(accessToken, refreshToken, "Bearer", expiresIn);
    }

    @Transactional(readOnly = true)
    public AuthenticationResponse refreshToken(TokenRefreshRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Token refresh started", kv("step", "start"));
        
        // Extract username from refresh token
        var refreshToken = request.refreshToken();
        var userEmail = jwtService.extractUsername(refreshToken);

        // Check for null or blank email
        if (userEmail == null || userEmail.isBlank()) {
            log.warn("Token refresh failed - invalid token (no email)", kv("step", "validation_failed"));
            throw new InvalidTokenException();
        }
        log.debug("Email extracted from refresh token", kv("email", userEmail), kv("step", "email_extracted"));
        
        // Get user and wrap in UserPrincipal
        var user = userRepository.findByEmail(userEmail)
                .orElseThrow(UserNotFoundException::new);
        UserDetails userDetails = new UserPrincipal(user);
        
        // Add userId to MDC for all subsequent logs
        MDC.put("userId", String.valueOf(user.getId()));
        log.debug("User found", kv("userId", user.getId()), kv("step", "user_found"));

        // Validate refresh token
        if (jwtService.isRefreshTokenValid(refreshToken, userDetails)) {
            long validationTime = System.currentTimeMillis();
            log.debug("Refresh token validated", 
                    kv("userId", user.getId()),
                    kv("step", "token_validated"),
                    kv("duration_ms", validationTime - startTime));
            
            // Generate new access token
            var accessToken = jwtService.generateToken(userDetails);
            long tokenTime = System.currentTimeMillis();
            log.debug("New access token generated", 
                    kv("userId", user.getId()),
                    kv("step", "token_generated"),
                    kv("duration_ms", tokenTime - validationTime));

            // Get JWT expiration time in seconds from application properties
            long expiresIn = jwtService.getJwtExpirationInSeconds();

            long totalTime = System.currentTimeMillis();
            log.info("Token refresh completed successfully", 
                    kv("userId", user.getId()),
                    kv("email", user.getEmail()),
                    kv("step", "complete"),
                    kv("duration_ms", totalTime - startTime));

            // Return response
            return new AuthenticationResponse(accessToken, refreshToken, "Bearer", expiresIn);
        }

        log.warn("Token refresh failed - invalid refresh token", 
                kv("userId", user.getId()), 
                kv("step", "validation_failed"));
        throw new InvalidTokenException();
    }
}
