package com.harrish.auth.security;

import com.harrish.auth.model.Role;
import com.harrish.auth.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserPrincipal")
class UserPrincipalTest {

    @Nested
    @DisplayName("Constructor and getUser()")
    class ConstructorTests {

        @Test
        @DisplayName("should wrap User entity and return it via getUser()")
        void shouldWrapUserEntity() {
            // Arrange
            User user = createUserWithRole(Role.USER);

            // Act
            UserPrincipal userPrincipal = new UserPrincipal(user);

            // Assert
            assertThat(userPrincipal.getUser()).isEqualTo(user);
        }
    }

    @Nested
    @DisplayName("getUsername()")
    class GetUsernameTests {

        @Test
        @DisplayName("should return user's email as username")
        void shouldReturnEmailAsUsername() {
            // Arrange
            User user = createUserWithRole(Role.USER);
            UserPrincipal userPrincipal = new UserPrincipal(user);

            // Act
            String username = userPrincipal.getUsername();

            // Assert
            assertThat(username).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("should return correct email for ADMIN user")
        void shouldReturnEmailForAdminUser() {
            // Arrange
            User adminUser = User.builder()
                    .id(2L)
                    .firstName("Admin")
                    .lastName("User")
                    .email("admin@example.com")
                    .password("encodedPassword")
                    .role(Role.ADMIN)
                    .build();
            UserPrincipal userPrincipal = new UserPrincipal(adminUser);

            // Act
            String username = userPrincipal.getUsername();

            // Assert
            assertThat(username).isEqualTo("admin@example.com");
        }
    }

    @Nested
    @DisplayName("getPassword()")
    class GetPasswordTests {

        @Test
        @DisplayName("should return user's password")
        void shouldReturnPassword() {
            // Arrange
            User user = createUserWithRole(Role.USER);
            UserPrincipal userPrincipal = new UserPrincipal(user);

            // Act
            String password = userPrincipal.getPassword();

            // Assert
            assertThat(password).isEqualTo("encodedPassword123");
        }

        @Test
        @DisplayName("should return correct password for ADMIN user")
        void shouldReturnPasswordForAdminUser() {
            // Arrange
            User adminUser = User.builder()
                    .id(2L)
                    .firstName("Admin")
                    .lastName("User")
                    .email("admin@example.com")
                    .password("adminEncodedPass")
                    .role(Role.ADMIN)
                    .build();
            UserPrincipal userPrincipal = new UserPrincipal(adminUser);

            // Act
            String password = userPrincipal.getPassword();

            // Assert
            assertThat(password).isEqualTo("adminEncodedPass");
        }
    }

    @Nested
    @DisplayName("getAuthorities()")
    class GetAuthoritiesTests {

        @Test
        @DisplayName("should return ROLE_USER authority for USER role")
        void shouldReturnUserAuthority() {
            // Arrange
            User user = createUserWithRole(Role.USER);
            UserPrincipal userPrincipal = new UserPrincipal(user);

            // Act
            Collection<? extends GrantedAuthority> authorities = userPrincipal.getAuthorities();

            // Assert
            assertThat(authorities)
                    .hasSize(1)
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_USER");
        }

        @Test
        @DisplayName("should return ROLE_ADMIN authority for ADMIN role")
        void shouldReturnAdminAuthority() {
            // Arrange
            User adminUser = createUserWithRole(Role.ADMIN);
            UserPrincipal userPrincipal = new UserPrincipal(adminUser);

            // Act
            Collection<? extends GrantedAuthority> authorities = userPrincipal.getAuthorities();

            // Assert
            assertThat(authorities)
                    .hasSize(1)
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_ADMIN");
        }

        @Test
        @DisplayName("should return authorities as non-null collection")
        void shouldReturnNonNullCollection() {
            // Arrange
            User user = createUserWithRole(Role.USER);
            UserPrincipal userPrincipal = new UserPrincipal(user);

            // Act
            Collection<? extends GrantedAuthority> authorities = userPrincipal.getAuthorities();

            // Assert
            assertThat(authorities).isNotNull();
        }

        @Test
        @DisplayName("should return authorities with correct authority string format")
        void shouldReturnCorrectAuthorityFormat() {
            // Arrange
            User user = createUserWithRole(Role.USER);
            UserPrincipal userPrincipal = new UserPrincipal(user);

            // Act
            Collection<? extends GrantedAuthority> authorities = userPrincipal.getAuthorities();

            // Assert
            assertThat(authorities)
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_USER");
        }
    }

    @Nested
    @DisplayName("Account Status Methods")
    class AccountStatusTests {

        @Test
        @DisplayName("isEnabled() should return true")
        void isEnabledShouldReturnTrue() {
            // Arrange
            User user = createUserWithRole(Role.USER);
            UserPrincipal userPrincipal = new UserPrincipal(user);

            // Act
            boolean enabled = userPrincipal.isEnabled();

            // Assert
            assertThat(enabled).isTrue();
        }

        @Test
        @DisplayName("isAccountNonExpired() should return true")
        void isAccountNonExpiredShouldReturnTrue() {
            // Arrange
            User user = createUserWithRole(Role.USER);
            UserPrincipal userPrincipal = new UserPrincipal(user);

            // Act
            boolean nonExpired = userPrincipal.isAccountNonExpired();

            // Assert
            assertThat(nonExpired).isTrue();
        }

        @Test
        @DisplayName("isAccountNonLocked() should return true")
        void isAccountNonLockedShouldReturnTrue() {
            // Arrange
            User user = createUserWithRole(Role.USER);
            UserPrincipal userPrincipal = new UserPrincipal(user);

            // Act
            boolean nonLocked = userPrincipal.isAccountNonLocked();

            // Assert
            assertThat(nonLocked).isTrue();
        }

        @Test
        @DisplayName("isCredentialsNonExpired() should return true")
        void isCredentialsNonExpiredShouldReturnTrue() {
            // Arrange
            User user = createUserWithRole(Role.USER);
            UserPrincipal userPrincipal = new UserPrincipal(user);

            // Act
            boolean credentialsNonExpired = userPrincipal.isCredentialsNonExpired();

            // Assert
            assertThat(credentialsNonExpired).isTrue();
        }

        @Test
        @DisplayName("all account flags should return true for ADMIN user")
        void allAccountFlagsShouldReturnTrueForAdmin() {
            // Arrange
            User adminUser = createUserWithRole(Role.ADMIN);
            UserPrincipal userPrincipal = new UserPrincipal(adminUser);

            // Act & Assert
            assertThat(userPrincipal.isEnabled()).isTrue();
            assertThat(userPrincipal.isAccountNonExpired()).isTrue();
            assertThat(userPrincipal.isAccountNonLocked()).isTrue();
            assertThat(userPrincipal.isCredentialsNonExpired()).isTrue();
        }

        @Test
        @DisplayName("all account flags should return true for USER role")
        void allAccountFlagsShouldReturnTrueForUser() {
            // Arrange
            User user = createUserWithRole(Role.USER);
            UserPrincipal userPrincipal = new UserPrincipal(user);

            // Act & Assert
            assertThat(userPrincipal.isEnabled()).isTrue();
            assertThat(userPrincipal.isAccountNonExpired()).isTrue();
            assertThat(userPrincipal.isAccountNonLocked()).isTrue();
            assertThat(userPrincipal.isCredentialsNonExpired()).isTrue();
        }
    }

    @Nested
    @DisplayName("Convenience Methods")
    class ConvenienceMethodsTests {

        @Test
        @DisplayName("getId() should return user's ID")
        void getIdShouldReturnUserId() {
            // Arrange
            User user = createUserWithRole(Role.USER);
            UserPrincipal userPrincipal = new UserPrincipal(user);

            // Act
            Long id = userPrincipal.getId();

            // Assert
            assertThat(id).isEqualTo(1L);
        }

        @Test
        @DisplayName("getEmail() should return user's email")
        void getEmailShouldReturnUserEmail() {
            // Arrange
            User user = createUserWithRole(Role.USER);
            UserPrincipal userPrincipal = new UserPrincipal(user);

            // Act
            String email = userPrincipal.getEmail();

            // Assert
            assertThat(email).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("getId() should return correct ID for ADMIN user")
        void getIdShouldReturnCorrectIdForAdmin() {
            // Arrange
            User adminUser = User.builder()
                    .id(99L)
                    .firstName("Admin")
                    .lastName("User")
                    .email("admin@example.com")
                    .password("adminPass")
                    .role(Role.ADMIN)
                    .build();
            UserPrincipal userPrincipal = new UserPrincipal(adminUser);

            // Act
            Long id = userPrincipal.getId();

            // Assert
            assertThat(id).isEqualTo(99L);
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringTests {

        @Test
        @DisplayName("should return formatted string with user details")
        void shouldReturnFormattedString() {
            // Arrange
            User user = createUserWithRole(Role.USER);
            UserPrincipal userPrincipal = new UserPrincipal(user);

            // Act
            String result = userPrincipal.toString();

            // Assert
            assertThat(result)
                    .contains("UserPrincipal")
                    .contains("userId=1")
                    .contains("email='test@example.com'")
                    .contains("role=USER");
        }

        @Test
        @DisplayName("should return formatted string for ADMIN user")
        void shouldReturnFormattedStringForAdmin() {
            // Arrange
            User adminUser = createUserWithRole(Role.ADMIN);
            UserPrincipal userPrincipal = new UserPrincipal(adminUser);

            // Act
            String result = userPrincipal.toString();

            // Assert
            assertThat(result)
                    .contains("UserPrincipal")
                    .contains("userId=1")
                    .contains("email='test@example.com'")
                    .contains("role=ADMIN");
        }
    }

    @Nested
    @DisplayName("Integration Tests with Different Roles")
    class IntegrationTests {

        @Test
        @DisplayName("should correctly handle USER role with all methods")
        void shouldHandleUserRoleCompletely() {
            // Arrange
            User user = User.builder()
                    .id(10L)
                    .firstName("John")
                    .lastName("Doe")
                    .email("john.doe@example.com")
                    .password("$2a$10$hashedPassword")
                    .role(Role.USER)
                    .build();

            // Act
            UserPrincipal userPrincipal = new UserPrincipal(user);

            // Assert
            assertThat(userPrincipal.getId()).isEqualTo(10L);
            assertThat(userPrincipal.getEmail()).isEqualTo("john.doe@example.com");
            assertThat(userPrincipal.getUsername()).isEqualTo("john.doe@example.com");
            assertThat(userPrincipal.getPassword()).isEqualTo("$2a$10$hashedPassword");
            assertThat(userPrincipal.getAuthorities())
                    .hasSize(1)
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_USER");
            assertThat(userPrincipal.isEnabled()).isTrue();
            assertThat(userPrincipal.isAccountNonExpired()).isTrue();
            assertThat(userPrincipal.isAccountNonLocked()).isTrue();
            assertThat(userPrincipal.isCredentialsNonExpired()).isTrue();
        }

        @Test
        @DisplayName("should correctly handle ADMIN role with all methods")
        void shouldHandleAdminRoleCompletely() {
            // Arrange
            User adminUser = User.builder()
                    .id(20L)
                    .firstName("Jane")
                    .lastName("Admin")
                    .email("jane.admin@example.com")
                    .password("$2a$10$adminHashedPassword")
                    .role(Role.ADMIN)
                    .build();

            // Act
            UserPrincipal userPrincipal = new UserPrincipal(adminUser);

            // Assert
            assertThat(userPrincipal.getId()).isEqualTo(20L);
            assertThat(userPrincipal.getEmail()).isEqualTo("jane.admin@example.com");
            assertThat(userPrincipal.getUsername()).isEqualTo("jane.admin@example.com");
            assertThat(userPrincipal.getPassword()).isEqualTo("$2a$10$adminHashedPassword");
            assertThat(userPrincipal.getAuthorities())
                    .hasSize(1)
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_ADMIN");
            assertThat(userPrincipal.isEnabled()).isTrue();
            assertThat(userPrincipal.isAccountNonExpired()).isTrue();
            assertThat(userPrincipal.isAccountNonLocked()).isTrue();
            assertThat(userPrincipal.isCredentialsNonExpired()).isTrue();
        }

        @Test
        @DisplayName("should differentiate between USER and ADMIN authorities")
        void shouldDifferentiateBetweenRoles() {
            // Arrange
            User regularUser = createUserWithRole(Role.USER);
            User adminUser = createUserWithRole(Role.ADMIN);

            // Act
            UserPrincipal userPrincipal = new UserPrincipal(regularUser);
            UserPrincipal adminPrincipal = new UserPrincipal(adminUser);

            // Assert
            assertThat(userPrincipal.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_USER");

            assertThat(adminPrincipal.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_ADMIN");

            assertThat(userPrincipal.getAuthorities())
                    .isNotEqualTo(adminPrincipal.getAuthorities());
        }
    }

    // Helper method to create test User objects
    private User createUserWithRole(Role role) {
        return User.builder()
                .id(1L)
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .password("encodedPassword123")
                .role(role)
                .build();
    }
}
