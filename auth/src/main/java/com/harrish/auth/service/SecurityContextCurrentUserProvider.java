package com.harrish.auth.service;

import com.harrish.auth.exception.UserNotFoundException;
import com.harrish.auth.model.User;
import com.harrish.auth.repository.UserRepository;
import com.harrish.auth.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Implementation of CurrentUserProvider that retrieves the user from Spring Security context.
 */
@Component
public class SecurityContextCurrentUserProvider implements CurrentUserProvider {

    private final UserRepository userRepository;

    public SecurityContextCurrentUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UserNotFoundException();
        }

        // Extract user from UserPrincipal
        if (authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getUser();
        }

        // Fallback: lookup by email if principal is a string
        if (authentication.getPrincipal() instanceof String email) {
            return userRepository.findByEmail(email)
                    .orElseThrow(UserNotFoundException::new);
        }

        throw new UserNotFoundException();
    }

    @Override
    public Optional<User> getCurrentUserIfPresent() {
        try {
            return Optional.of(getCurrentUser());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
