package com.harrish.auth.config

import com.harrish.auth.model.User
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.security.core.context.SecurityContextHolder
import java.util.Optional

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
class JpaAuditingConfig {

    @Bean
    fun auditorProvider(): AuditorAware<User> {
        return Optional.ofNullable(SecurityContextHolder.getContext().authentication)
            .filter { it.isAuthenticated }
            .map { it.principal as? User }
            .orElse(null)?.let { Optional.of(it) }
            ?: Optional.empty()
    }
}
