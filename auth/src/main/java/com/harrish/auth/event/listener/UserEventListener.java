package com.harrish.auth.event.listener;

import com.harrish.auth.event.UserRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event listener for user-related domain events.
 * Handles cross-cutting concerns like audit logging, notifications, and analytics
 * in a decoupled manner using the Observer pattern.
 * 
 * Benefits:
 * - Decouples user registration logic from side effects
 * - Makes it easy to add new behaviors without modifying core service
 * - Async processing prevents blocking the main registration flow
 * - Single Responsibility: each listener method handles one concern
 */
@Slf4j
@Component
public class UserEventListener {

    /**
     * Handles user registration events.
     * Logs the registration for audit purposes and could trigger additional actions
     * like sending welcome emails, initializing user preferences, or updating analytics.
     * 
     * @param event the user registration event
     */
    @Async
    @EventListener
    public void handleUserRegistered(UserRegisteredEvent event) {
        var user = event.getUser();
        log.info("User registered - ID: {}, Username: {}, Email: {}, Role: {}, Timestamp: {}",
                user.getId(),
                user.getFirstName() + " " + user.getLastName(),
                user.getEmail(),
                user.getRole(),
                event.getRegisteredAt());
        
        // TODO: Future enhancements can be added here without modifying AuthenticationService:
        // - Send welcome email via email service
        // - Initialize default user preferences
        // - Trigger analytics/metrics collection
        // - Create default user workspace/profile
        // - Add to mailing list
        // - Send notification to admin for new user approvals (if needed)
    }
    
    /**
     * Example method showing how easy it is to add new event handlers
     * for the same event type to handle different concerns.
     */
    @Async
    @EventListener
    public void trackUserRegistrationMetrics(UserRegisteredEvent event) {
        log.debug("Tracking registration metrics for user: {}", event.getUser().getEmail());
        // TODO: Send metrics to analytics service
        // - Track registration source
        // - Update user count metrics
        // - Track registration time patterns
    }
}
