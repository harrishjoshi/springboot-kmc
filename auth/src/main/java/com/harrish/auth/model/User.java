package com.harrish.auth.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

/**
 * User domain entity representing a user in the system.
 * This is a pure JPA entity without Spring Security concerns.
 * For Spring Security integration, use {@link com.harrish.auth.security.UserPrincipal}.
 * 
 * <p>Note: This entity does not expose setters. Use domain methods for mutations:
 * <ul>
 *   <li>{@link #changePassword(String)} - to update password</li>
 *   <li>{@link #updateProfile(String, String)} - to update profile info</li>
 * </ul>
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email", unique = true)
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /**
     * Changes the user's password.
     * 
     * @param newPassword the new password (should already be encoded)
     */
    public void changePassword(String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("Password cannot be blank");
        }
        this.password = newPassword;
    }

    /**
     * Updates the user's profile information.
     * 
     * @param firstName the new first name
     * @param lastName the new last name
     */
    public void updateProfile(String firstName, String lastName) {
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("First name cannot be blank");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Last name cannot be blank");
        }
        this.firstName = firstName;
        this.lastName = lastName;
    }
    
    // Use only id for equals/hashCode to work properly with JPA proxies
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return id != null && Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                '}';
    }
}
