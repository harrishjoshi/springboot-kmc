package com.harrish.auth.event;

import com.harrish.auth.model.User;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * Event published when a new user registers in the system.
 * This enables decoupled handling of registration side effects:
 * - Sending welcome emails
 * - Logging audit trails  
 * - Notifying administrators
 * - Tracking analytics
 */
public class UserRegisteredEvent extends ApplicationEvent {
    
    private final User user;
    private final LocalDateTime registeredAt;

    public UserRegisteredEvent(Object source, User user) {
        super(source);
        this.user = user;
        this.registeredAt = LocalDateTime.now();
    }

    public User getUser() {
        return user;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    @Override
    public String toString() {
        return "UserRegisteredEvent{" +
                "user=" + user.getEmail() +
                ", registeredAt=" + registeredAt +
                '}';
    }
}
