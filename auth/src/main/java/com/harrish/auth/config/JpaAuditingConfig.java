package com.harrish.auth.config;

import com.harrish.auth.model.User;
import com.harrish.auth.service.CurrentUserProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * JPA Auditing configuration.
 * Now uses CurrentUserProvider abstraction instead of direct SecurityContext access.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<User> auditorProvider(CurrentUserProvider currentUserProvider) {
        return () -> {
            try {
                return Optional.of(currentUserProvider.getCurrentUser());
            } catch (Exception e) {
                // Return empty if no authenticated user (e.g., during system operations or tests)
                return Optional.empty();
            }
        };
    }
}