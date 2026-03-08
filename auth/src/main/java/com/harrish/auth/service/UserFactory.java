package com.harrish.auth.service;

import com.harrish.auth.dto.RegisterRequest;
import com.harrish.auth.model.Role;
import com.harrish.auth.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Factory for creating User entities using the Factory Method pattern.
 * 
 * Benefits:
 * - Encapsulates complex user creation logic in one place
 * - Ensures consistent user creation across the application
 * - Makes it easy to add new user types without modifying service classes
 * - Centralizes password encoding logic
 * - Makes user creation logic testable independently
 * - Follows Single Responsibility Principle
 * 
 * This factory replaces scattered User.builder() calls throughout the codebase
 * with intention-revealing factory methods.
 */
@Component
@RequiredArgsConstructor
public class UserFactory {

    private final PasswordEncoder passwordEncoder;

    /**
     * Creates a standard user with USER role from registration request.
     * This is the most common user creation path.
     * 
     * @param request the registration request containing user details
     * @return a new User entity with USER role and encoded password
     */
    public User createStandardUser(RegisterRequest request) {
        return createUserWithRole(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.password(),
                Role.USER
        );
    }

    /**
     * Creates an admin user with ADMIN role.
     * Can be used for administrative operations or seed data.
     * 
     * @param firstName the user's first name
     * @param lastName the user's last name
     * @param email the user's email address
     * @param rawPassword the user's password (will be encoded)
     * @return a new User entity with ADMIN role and encoded password
     */
    public User createAdminUser(String firstName, String lastName, String email, String rawPassword) {
        return createUserWithRole(firstName, lastName, email, rawPassword, Role.ADMIN);
    }

    /**
     * Creates a user with a specific role.
     * This is the core factory method that all other methods delegate to.
     * 
     * @param firstName the user's first name
     * @param lastName the user's last name
     * @param email the user's email address
     * @param rawPassword the user's password (will be encoded)
     * @param role the role to assign to the user
     * @return a new User entity with the specified role and encoded password
     */
    public User createUserWithRole(
            String firstName,
            String lastName,
            String email,
            String rawPassword,
            Role role
    ) {
        String encodedPassword = passwordEncoder.encode(rawPassword);
        
        return User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .password(encodedPassword)
                .role(role)
                .build();
    }
}
