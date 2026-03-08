package com.harrish.auth.service;

import com.harrish.auth.dto.RegisterRequest;
import com.harrish.auth.model.Role;
import com.harrish.auth.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserFactory")
class UserFactoryTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserFactory userFactory;

    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";
    private static final String EMAIL = "john.doe@example.com";
    private static final String RAW_PASSWORD = "Password123!";
    private static final String ENCODED_PASSWORD = "encoded_Password123!";

    @BeforeEach
    void setUp() {
        // Mock password encoder to return a predictable encoded password
        when(passwordEncoder.encode(anyString()))
                .thenAnswer(invocation -> "encoded_" + invocation.getArgument(0));
    }

    @Nested
    @DisplayName("createStandardUser")
    class CreateStandardUserTests {

        @Test
        @DisplayName("should create user with USER role from RegisterRequest")
        void shouldCreateUserWithUserRole() {
            // Arrange
            RegisterRequest request = new RegisterRequest(
                    FIRST_NAME,
                    LAST_NAME,
                    EMAIL,
                    RAW_PASSWORD
            );

            // Act
            User user = userFactory.createStandardUser(request);

            // Assert
            assertThat(user).isNotNull();
            assertThat(user.getFirstName()).isEqualTo(FIRST_NAME);
            assertThat(user.getLastName()).isEqualTo(LAST_NAME);
            assertThat(user.getEmail()).isEqualTo(EMAIL);
            assertThat(user.getRole()).isEqualTo(Role.USER);
        }

        @Test
        @DisplayName("should encode password using PasswordEncoder")
        void shouldEncodePassword() {
            // Arrange
            RegisterRequest request = new RegisterRequest(
                    FIRST_NAME,
                    LAST_NAME,
                    EMAIL,
                    RAW_PASSWORD
            );

            // Act
            User user = userFactory.createStandardUser(request);

            // Assert
            verify(passwordEncoder, times(1)).encode(RAW_PASSWORD);
            assertThat(user.getPassword()).isEqualTo(ENCODED_PASSWORD);
            assertThat(user.getPassword()).isNotEqualTo(RAW_PASSWORD);
        }

        @Test
        @DisplayName("should create user with all fields populated correctly")
        void shouldPopulateAllFieldsCorrectly() {
            // Arrange
            RegisterRequest request = new RegisterRequest(
                    "Alice",
                    "Smith",
                    "alice.smith@example.com",
                    "SecurePass456!"
            );

            // Act
            User user = userFactory.createStandardUser(request);

            // Assert
            assertThat(user.getFirstName()).isEqualTo("Alice");
            assertThat(user.getLastName()).isEqualTo("Smith");
            assertThat(user.getEmail()).isEqualTo("alice.smith@example.com");
            assertThat(user.getPassword()).isEqualTo("encoded_SecurePass456!");
            assertThat(user.getRole()).isEqualTo(Role.USER);
        }

        @Test
        @DisplayName("should call passwordEncoder.encode exactly once")
        void shouldCallPasswordEncoderOnce() {
            // Arrange
            RegisterRequest request = new RegisterRequest(
                    FIRST_NAME,
                    LAST_NAME,
                    EMAIL,
                    RAW_PASSWORD
            );

            // Act
            userFactory.createStandardUser(request);

            // Assert
            verify(passwordEncoder, times(1)).encode(RAW_PASSWORD);
            verifyNoMoreInteractions(passwordEncoder);
        }
    }

    @Nested
    @DisplayName("createAdminUser")
    class CreateAdminUserTests {

        @Test
        @DisplayName("should create user with ADMIN role")
        void shouldCreateUserWithAdminRole() {
            // Arrange
            // (using constants from outer class)

            // Act
            User user = userFactory.createAdminUser(FIRST_NAME, LAST_NAME, EMAIL, RAW_PASSWORD);

            // Assert
            assertThat(user).isNotNull();
            assertThat(user.getFirstName()).isEqualTo(FIRST_NAME);
            assertThat(user.getLastName()).isEqualTo(LAST_NAME);
            assertThat(user.getEmail()).isEqualTo(EMAIL);
            assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        }

        @Test
        @DisplayName("should encode password using PasswordEncoder")
        void shouldEncodePassword() {
            // Arrange
            // (using constants from outer class)

            // Act
            User user = userFactory.createAdminUser(FIRST_NAME, LAST_NAME, EMAIL, RAW_PASSWORD);

            // Assert
            verify(passwordEncoder, times(1)).encode(RAW_PASSWORD);
            assertThat(user.getPassword()).isEqualTo(ENCODED_PASSWORD);
            assertThat(user.getPassword()).isNotEqualTo(RAW_PASSWORD);
        }

        @Test
        @DisplayName("should create admin user with all fields populated correctly")
        void shouldPopulateAllFieldsCorrectly() {
            // Arrange
            String firstName = "Bob";
            String lastName = "Admin";
            String email = "bob.admin@example.com";
            String password = "AdminPass789!";

            // Act
            User user = userFactory.createAdminUser(firstName, lastName, email, password);

            // Assert
            assertThat(user.getFirstName()).isEqualTo("Bob");
            assertThat(user.getLastName()).isEqualTo("Admin");
            assertThat(user.getEmail()).isEqualTo("bob.admin@example.com");
            assertThat(user.getPassword()).isEqualTo("encoded_AdminPass789!");
            assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        }

        @Test
        @DisplayName("should call passwordEncoder.encode exactly once")
        void shouldCallPasswordEncoderOnce() {
            // Arrange
            // (using constants from outer class)

            // Act
            userFactory.createAdminUser(FIRST_NAME, LAST_NAME, EMAIL, RAW_PASSWORD);

            // Assert
            verify(passwordEncoder, times(1)).encode(RAW_PASSWORD);
            verifyNoMoreInteractions(passwordEncoder);
        }
    }

    @Nested
    @DisplayName("createUserWithRole")
    class CreateUserWithRoleTests {

        @Test
        @DisplayName("should create user with specified USER role")
        void shouldCreateUserWithUserRole() {
            // Arrange
            // (using constants from outer class)

            // Act
            User user = userFactory.createUserWithRole(
                    FIRST_NAME,
                    LAST_NAME,
                    EMAIL,
                    RAW_PASSWORD,
                    Role.USER
            );

            // Assert
            assertThat(user).isNotNull();
            assertThat(user.getFirstName()).isEqualTo(FIRST_NAME);
            assertThat(user.getLastName()).isEqualTo(LAST_NAME);
            assertThat(user.getEmail()).isEqualTo(EMAIL);
            assertThat(user.getRole()).isEqualTo(Role.USER);
        }

        @Test
        @DisplayName("should create user with specified ADMIN role")
        void shouldCreateUserWithAdminRole() {
            // Arrange
            // (using constants from outer class)

            // Act
            User user = userFactory.createUserWithRole(
                    FIRST_NAME,
                    LAST_NAME,
                    EMAIL,
                    RAW_PASSWORD,
                    Role.ADMIN
            );

            // Assert
            assertThat(user).isNotNull();
            assertThat(user.getFirstName()).isEqualTo(FIRST_NAME);
            assertThat(user.getLastName()).isEqualTo(LAST_NAME);
            assertThat(user.getEmail()).isEqualTo(EMAIL);
            assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        }

        @Test
        @DisplayName("should encode password using PasswordEncoder")
        void shouldEncodePassword() {
            // Arrange
            // (using constants from outer class)

            // Act
            User user = userFactory.createUserWithRole(
                    FIRST_NAME,
                    LAST_NAME,
                    EMAIL,
                    RAW_PASSWORD,
                    Role.USER
            );

            // Assert
            verify(passwordEncoder, times(1)).encode(RAW_PASSWORD);
            assertThat(user.getPassword()).isEqualTo(ENCODED_PASSWORD);
            assertThat(user.getPassword()).isNotEqualTo(RAW_PASSWORD);
        }

        @Test
        @DisplayName("should create user with all fields populated correctly")
        void shouldPopulateAllFieldsCorrectly() {
            // Arrange
            String firstName = "Charlie";
            String lastName = "Brown";
            String email = "charlie.brown@example.com";
            String password = "CharliePwd999!";
            Role role = Role.USER;

            // Act
            User user = userFactory.createUserWithRole(firstName, lastName, email, password, role);

            // Assert
            assertThat(user.getFirstName()).isEqualTo("Charlie");
            assertThat(user.getLastName()).isEqualTo("Brown");
            assertThat(user.getEmail()).isEqualTo("charlie.brown@example.com");
            assertThat(user.getPassword()).isEqualTo("encoded_CharliePwd999!");
            assertThat(user.getRole()).isEqualTo(Role.USER);
        }

        @Test
        @DisplayName("should call passwordEncoder.encode exactly once")
        void shouldCallPasswordEncoderOnce() {
            // Arrange
            // (using constants from outer class)

            // Act
            userFactory.createUserWithRole(FIRST_NAME, LAST_NAME, EMAIL, RAW_PASSWORD, Role.ADMIN);

            // Assert
            verify(passwordEncoder, times(1)).encode(RAW_PASSWORD);
            verifyNoMoreInteractions(passwordEncoder);
        }

        @Test
        @DisplayName("should handle different passwords correctly")
        void shouldHandleDifferentPasswords() {
            // Arrange
            String password1 = "Pass1!";
            String password2 = "Pass2!";

            // Act
            User user1 = userFactory.createUserWithRole(FIRST_NAME, LAST_NAME, EMAIL, password1, Role.USER);
            User user2 = userFactory.createUserWithRole(FIRST_NAME, LAST_NAME, EMAIL, password2, Role.ADMIN);

            // Assert
            assertThat(user1.getPassword()).isEqualTo("encoded_Pass1!");
            assertThat(user2.getPassword()).isEqualTo("encoded_Pass2!");
            assertThat(user1.getPassword()).isNotEqualTo(user2.getPassword());
            verify(passwordEncoder, times(1)).encode(password1);
            verify(passwordEncoder, times(1)).encode(password2);
        }
    }

    @Nested
    @DisplayName("Integration - Password Encoding")
    class PasswordEncodingIntegrationTests {

        @Test
        @DisplayName("should always encode raw passwords and never store them in plain text")
        void shouldNeverStorePlainTextPassword() {
            // Arrange
            String rawPassword = "PlainTextPassword123!";
            RegisterRequest request = new RegisterRequest(FIRST_NAME, LAST_NAME, EMAIL, rawPassword);

            // Act
            User standardUser = userFactory.createStandardUser(request);
            User adminUser = userFactory.createAdminUser(FIRST_NAME, LAST_NAME, EMAIL, rawPassword);
            User customUser = userFactory.createUserWithRole(FIRST_NAME, LAST_NAME, EMAIL, rawPassword, Role.USER);

            // Assert
            assertThat(standardUser.getPassword()).isNotEqualTo(rawPassword);
            assertThat(adminUser.getPassword()).isNotEqualTo(rawPassword);
            assertThat(customUser.getPassword()).isNotEqualTo(rawPassword);
            assertThat(standardUser.getPassword()).startsWith("encoded_");
            assertThat(adminUser.getPassword()).startsWith("encoded_");
            assertThat(customUser.getPassword()).startsWith("encoded_");
        }

        @Test
        @DisplayName("should use passwordEncoder for all user creation methods")
        void shouldUsePasswordEncoderForAllMethods() {
            // Arrange
            RegisterRequest request = new RegisterRequest(FIRST_NAME, LAST_NAME, EMAIL, "Pass1!");
            String password2 = "Pass2!";
            String password3 = "Pass3!";

            // Act
            userFactory.createStandardUser(request);
            userFactory.createAdminUser(FIRST_NAME, LAST_NAME, EMAIL, password2);
            userFactory.createUserWithRole(FIRST_NAME, LAST_NAME, EMAIL, password3, Role.USER);

            // Assert
            verify(passwordEncoder, times(1)).encode("Pass1!");
            verify(passwordEncoder, times(1)).encode(password2);
            verify(passwordEncoder, times(1)).encode(password3);
            verify(passwordEncoder, times(3)).encode(anyString());
        }
    }

    @Nested
    @DisplayName("Integration - Role Assignment")
    class RoleAssignmentIntegrationTests {

        @Test
        @DisplayName("createStandardUser should always assign USER role")
        void standardUserShouldAlwaysHaveUserRole() {
            // Arrange
            RegisterRequest request = new RegisterRequest(FIRST_NAME, LAST_NAME, EMAIL, RAW_PASSWORD);

            // Act
            User user = userFactory.createStandardUser(request);

            // Assert
            assertThat(user.getRole()).isEqualTo(Role.USER);
            assertThat(user.getRole()).isNotEqualTo(Role.ADMIN);
        }

        @Test
        @DisplayName("createAdminUser should always assign ADMIN role")
        void adminUserShouldAlwaysHaveAdminRole() {
            // Arrange
            // (using constants from outer class)

            // Act
            User user = userFactory.createAdminUser(FIRST_NAME, LAST_NAME, EMAIL, RAW_PASSWORD);

            // Assert
            assertThat(user.getRole()).isEqualTo(Role.ADMIN);
            assertThat(user.getRole()).isNotEqualTo(Role.USER);
        }

        @Test
        @DisplayName("createUserWithRole should assign exactly the role specified")
        void customUserShouldHaveSpecifiedRole() {
            // Arrange
            // (using constants from outer class)

            // Act
            User userWithUserRole = userFactory.createUserWithRole(
                    FIRST_NAME, LAST_NAME, EMAIL, RAW_PASSWORD, Role.USER
            );
            User userWithAdminRole = userFactory.createUserWithRole(
                    FIRST_NAME, LAST_NAME, "admin@example.com", RAW_PASSWORD, Role.ADMIN
            );

            // Assert
            assertThat(userWithUserRole.getRole()).isEqualTo(Role.USER);
            assertThat(userWithAdminRole.getRole()).isEqualTo(Role.ADMIN);
        }
    }
}
