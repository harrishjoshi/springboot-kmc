package com.harrish.auth.security;

import com.harrish.auth.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adapter class that wraps a User entity and implements Spring Security's UserDetails interface.
 * This separates domain concerns (User) from security concerns (UserDetails).
 */
public class UserPrincipal implements UserDetails {

    /**
     * The wrapped User entity.
     */
    private final User user;

    public UserPrincipal(User user) {
        this.user = user;
    }

    /**
     * Returns the wrapped User entity.
     * 
     * @return the User entity
     */
    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        // Email is used as the username
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Can be extended to check user.getAccountStatus() if needed
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Can be extended to check user.isLocked() if needed
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Can be extended to check user.getPasswordExpiry() if needed
    }

    @Override
    public boolean isEnabled() {
        return true; // Can be extended to check user.isEnabled() if needed
    }

    /**
     * Convenience method to get the user ID.
     * 
     * @return the user's ID
     */
    public Long getId() {
        return user.getId();
    }

    /**
     * Convenience method to get the user's email.
     * 
     * @return the user's email
     */
    public String getEmail() {
        return user.getEmail();
    }

    @Override
    public String toString() {
        return "UserPrincipal{" +
                "userId=" + user.getId() +
                ", email='" + user.getEmail() + '\'' +
                ", role=" + user.getRole() +
                '}';
    }
}
