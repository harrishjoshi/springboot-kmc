package com.harrish.auth.service;

import com.harrish.auth.model.User;

import java.util.Optional;

/**
 * Abstraction for accessing the current authenticated user.
 * This interface decouples business logic from Spring Security infrastructure.
 */
public interface CurrentUserProvider {
    
    /**
     * Gets the current authenticated user.
     * 
     * @return the current user
     * @throws com.harrish.auth.exception.UserNotFoundException if no authenticated user found
     * @throws org.springframework.security.core.AuthenticationException if not authenticated
     */
    User getCurrentUser();
    
    /**
     * Gets the current authenticated user if present.
     * 
     * @return Optional containing the current user, or empty if not authenticated
     */
    Optional<User> getCurrentUserIfPresent();
}
