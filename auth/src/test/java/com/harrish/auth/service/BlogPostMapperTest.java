package com.harrish.auth.service;

import com.harrish.auth.dto.BlogPostResponse;
import com.harrish.auth.dto.UserDto;
import com.harrish.auth.model.BlogPost;
import com.harrish.auth.model.Role;
import com.harrish.auth.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BlogPostMapper")
class BlogPostMapperTest {

    private BlogPostMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new BlogPostMapper();
    }

    @Nested
    @DisplayName("toResponse")
    class ToResponse {

        @Test
        @DisplayName("should map BlogPost entity to BlogPostResponse with all fields")
        void shouldMapBlogPostToResponse() {
            // Arrange
            User creator = createUser(1L, "John", "Doe", "john.doe@example.com");
            User updater = createUser(2L, "Jane", "Smith", "jane.smith@example.com");
            LocalDateTime createdAt = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
            LocalDateTime updatedAt = LocalDateTime.of(2024, 1, 16, 14, 45, 0);

            BlogPost blogPost = BlogPost.builder()
                    .id(100L)
                    .title("Test Blog Post")
                    .content("This is the blog post content")
                    .createdAt(createdAt)
                    .updatedAt(updatedAt)
                    .createdBy(creator)
                    .updatedBy(updater)
                    .build();

            // Act
            BlogPostResponse response = mapper.toResponse(blogPost);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(100L);
            assertThat(response.getTitle()).isEqualTo("Test Blog Post");
            assertThat(response.getContent()).isEqualTo("This is the blog post content");
            assertThat(response.getCreatedAt()).isEqualTo(createdAt);
            assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);

            assertThat(response.getCreatedBy()).isNotNull();
            assertThat(response.getCreatedBy().getId()).isEqualTo(1L);
            assertThat(response.getCreatedBy().getFirstName()).isEqualTo("John");
            assertThat(response.getCreatedBy().getLastName()).isEqualTo("Doe");
            assertThat(response.getCreatedBy().getEmail()).isEqualTo("john.doe@example.com");

            assertThat(response.getUpdatedBy()).isNotNull();
            assertThat(response.getUpdatedBy().getId()).isEqualTo(2L);
            assertThat(response.getUpdatedBy().getFirstName()).isEqualTo("Jane");
            assertThat(response.getUpdatedBy().getLastName()).isEqualTo("Smith");
            assertThat(response.getUpdatedBy().getEmail()).isEqualTo("jane.smith@example.com");
        }

        @Test
        @DisplayName("should map BlogPost entity with null createdBy and updatedBy")
        void shouldMapBlogPostWithNullUsers() {
            // Arrange
            LocalDateTime createdAt = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
            LocalDateTime updatedAt = LocalDateTime.of(2024, 1, 16, 14, 45, 0);

            BlogPost blogPost = BlogPost.builder()
                    .id(100L)
                    .title("Test Blog Post")
                    .content("This is the blog post content")
                    .createdAt(createdAt)
                    .updatedAt(updatedAt)
                    .createdBy(null)
                    .updatedBy(null)
                    .build();

            // Act
            BlogPostResponse response = mapper.toResponse(blogPost);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(100L);
            assertThat(response.getTitle()).isEqualTo("Test Blog Post");
            assertThat(response.getContent()).isEqualTo("This is the blog post content");
            assertThat(response.getCreatedAt()).isEqualTo(createdAt);
            assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
            assertThat(response.getCreatedBy()).isNull();
            assertThat(response.getUpdatedBy()).isNull();
        }

        @Test
        @DisplayName("should map BlogPost entity with same creator and updater")
        void shouldMapBlogPostWithSameCreatorAndUpdater() {
            // Arrange
            User user = createUser(1L, "John", "Doe", "john.doe@example.com");
            LocalDateTime createdAt = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
            LocalDateTime updatedAt = LocalDateTime.of(2024, 1, 16, 14, 45, 0);

            BlogPost blogPost = BlogPost.builder()
                    .id(100L)
                    .title("Test Blog Post")
                    .content("This is the blog post content")
                    .createdAt(createdAt)
                    .updatedAt(updatedAt)
                    .createdBy(user)
                    .updatedBy(user)
                    .build();

            // Act
            BlogPostResponse response = mapper.toResponse(blogPost);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getCreatedBy()).isNotNull();
            assertThat(response.getUpdatedBy()).isNotNull();
            assertThat(response.getCreatedBy().getId()).isEqualTo(response.getUpdatedBy().getId());
            assertThat(response.getCreatedBy().getEmail()).isEqualTo("john.doe@example.com");
            assertThat(response.getUpdatedBy().getEmail()).isEqualTo("john.doe@example.com");
        }

        @Test
        @DisplayName("should map BlogPost with long content")
        void shouldMapBlogPostWithLongContent() {
            // Arrange
            String longContent = "A".repeat(5000);
            BlogPost blogPost = BlogPost.builder()
                    .id(100L)
                    .title("Long Content Post")
                    .content(longContent)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            // Act
            BlogPostResponse response = mapper.toResponse(blogPost);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(5000);
            assertThat(response.getContent()).isEqualTo(longContent);
        }
    }

    @Nested
    @DisplayName("toResponseList")
    class ToResponseList {

        @Test
        @DisplayName("should map multiple BlogPost entities to BlogPostResponse list")
        void shouldMapMultipleBlogPostsToResponseList() {
            // Arrange
            User creator = createUser(1L, "John", "Doe", "john.doe@example.com");
            LocalDateTime now = LocalDateTime.now();

            BlogPost blogPost1 = BlogPost.builder()
                    .id(1L)
                    .title("First Post")
                    .content("First content")
                    .createdAt(now)
                    .updatedAt(now)
                    .createdBy(creator)
                    .build();

            BlogPost blogPost2 = BlogPost.builder()
                    .id(2L)
                    .title("Second Post")
                    .content("Second content")
                    .createdAt(now.plusDays(1))
                    .updatedAt(now.plusDays(1))
                    .createdBy(creator)
                    .build();

            BlogPost blogPost3 = BlogPost.builder()
                    .id(3L)
                    .title("Third Post")
                    .content("Third content")
                    .createdAt(now.plusDays(2))
                    .updatedAt(now.plusDays(2))
                    .createdBy(creator)
                    .build();

            List<BlogPost> blogPosts = List.of(blogPost1, blogPost2, blogPost3);

            // Act
            List<BlogPostResponse> responses = mapper.toResponseList(blogPosts);

            // Assert
            assertThat(responses).isNotNull();
            assertThat(responses).hasSize(3);

            assertThat(responses.get(0).getId()).isEqualTo(1L);
            assertThat(responses.get(0).getTitle()).isEqualTo("First Post");
            assertThat(responses.get(0).getContent()).isEqualTo("First content");

            assertThat(responses.get(1).getId()).isEqualTo(2L);
            assertThat(responses.get(1).getTitle()).isEqualTo("Second Post");
            assertThat(responses.get(1).getContent()).isEqualTo("Second content");

            assertThat(responses.get(2).getId()).isEqualTo(3L);
            assertThat(responses.get(2).getTitle()).isEqualTo("Third Post");
            assertThat(responses.get(2).getContent()).isEqualTo("Third content");
        }

        @Test
        @DisplayName("should return empty list when given empty BlogPost list")
        void shouldReturnEmptyListWhenGivenEmptyList() {
            // Arrange
            List<BlogPost> emptyList = List.of();

            // Act
            List<BlogPostResponse> responses = mapper.toResponseList(emptyList);

            // Assert
            assertThat(responses).isNotNull();
            assertThat(responses).isEmpty();
        }

        @Test
        @DisplayName("should map single BlogPost in list")
        void shouldMapSingleBlogPostInList() {
            // Arrange
            BlogPost blogPost = BlogPost.builder()
                    .id(1L)
                    .title("Single Post")
                    .content("Single content")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            List<BlogPost> blogPosts = List.of(blogPost);

            // Act
            List<BlogPostResponse> responses = mapper.toResponseList(blogPosts);

            // Assert
            assertThat(responses).isNotNull();
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getId()).isEqualTo(1L);
            assertThat(responses.get(0).getTitle()).isEqualTo("Single Post");
        }

        @Test
        @DisplayName("should preserve order when mapping BlogPost list")
        void shouldPreserveOrderWhenMappingList() {
            // Arrange
            List<BlogPost> blogPosts = List.of(
                    createBlogPost(5L, "Fifth"),
                    createBlogPost(3L, "Third"),
                    createBlogPost(1L, "First"),
                    createBlogPost(4L, "Fourth"),
                    createBlogPost(2L, "Second")
            );

            // Act
            List<BlogPostResponse> responses = mapper.toResponseList(blogPosts);

            // Assert
            assertThat(responses).hasSize(5);
            assertThat(responses)
                    .extracting(BlogPostResponse::getId)
                    .containsExactly(5L, 3L, 1L, 4L, 2L);
            assertThat(responses)
                    .extracting(BlogPostResponse::getTitle)
                    .containsExactly("Fifth", "Third", "First", "Fourth", "Second");
        }
    }

    @Nested
    @DisplayName("toUserDto")
    class ToUserDto {

        @Test
        @DisplayName("should map User entity to UserDto with all fields")
        void shouldMapUserToUserDto() {
            // Arrange
            User user = createUser(1L, "John", "Doe", "john.doe@example.com");

            // Act
            UserDto userDto = mapper.toUserDto(user);

            // Assert
            assertThat(userDto).isNotNull();
            assertThat(userDto.getId()).isEqualTo(1L);
            assertThat(userDto.getFirstName()).isEqualTo("John");
            assertThat(userDto.getLastName()).isEqualTo("Doe");
            assertThat(userDto.getEmail()).isEqualTo("john.doe@example.com");
        }

        @Test
        @DisplayName("should return null when User is null")
        void shouldReturnNullWhenUserIsNull() {
            // Arrange
            User user = null;

            // Act
            UserDto userDto = mapper.toUserDto(user);

            // Assert
            assertThat(userDto).isNull();
        }

        @Test
        @DisplayName("should map User with special characters in name")
        void shouldMapUserWithSpecialCharactersInName() {
            // Arrange
            User user = User.builder()
                    .id(1L)
                    .firstName("Jean-Pierre")
                    .lastName("O'Connor")
                    .email("jean.oconnor@example.com")
                    .password("encoded-password")
                    .role(Role.USER)
                    .build();

            // Act
            UserDto userDto = mapper.toUserDto(user);

            // Assert
            assertThat(userDto).isNotNull();
            assertThat(userDto.getFirstName()).isEqualTo("Jean-Pierre");
            assertThat(userDto.getLastName()).isEqualTo("O'Connor");
            assertThat(userDto.getEmail()).isEqualTo("jean.oconnor@example.com");
        }

        @Test
        @DisplayName("should not expose password in UserDto")
        void shouldNotExposePasswordInUserDto() {
            // Arrange
            User user = User.builder()
                    .id(1L)
                    .firstName("John")
                    .lastName("Doe")
                    .email("john.doe@example.com")
                    .password("secret-password")
                    .role(Role.USER)
                    .build();

            // Act
            UserDto userDto = mapper.toUserDto(user);

            // Assert
            assertThat(userDto).isNotNull();
            assertThat(userDto.toString()).doesNotContain("password");
            assertThat(userDto.toString()).doesNotContain("secret-password");
        }

        @Test
        @DisplayName("should map User with different roles consistently")
        void shouldMapUserWithDifferentRoles() {
            // Arrange
            User adminUser = createUser(1L, "Admin", "User", "admin@example.com", Role.ADMIN);
            User regularUser = createUser(2L, "Regular", "User", "user@example.com", Role.USER);

            // Act
            UserDto adminDto = mapper.toUserDto(adminUser);
            UserDto regularDto = mapper.toUserDto(regularUser);

            // Assert
            assertThat(adminDto).isNotNull();
            assertThat(adminDto.getId()).isEqualTo(1L);
            assertThat(adminDto.getFirstName()).isEqualTo("Admin");

            assertThat(regularDto).isNotNull();
            assertThat(regularDto.getId()).isEqualTo(2L);
            assertThat(regularDto.getFirstName()).isEqualTo("Regular");
        }
    }

    // Test data builders

    /**
     * Creates a User entity for testing purposes.
     *
     * @param id        the user ID
     * @param firstName the first name
     * @param lastName  the last name
     * @param email     the email address
     * @return a User entity
     */
    private User createUser(Long id, String firstName, String lastName, String email) {
        return createUser(id, firstName, lastName, email, Role.USER);
    }

    /**
     * Creates a User entity with a specific role for testing purposes.
     *
     * @param id        the user ID
     * @param firstName the first name
     * @param lastName  the last name
     * @param email     the email address
     * @param role      the user role
     * @return a User entity
     */
    private User createUser(Long id, String firstName, String lastName, String email, Role role) {
        return User.builder()
                .id(id)
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .password("encoded-password-" + id)
                .role(role)
                .build();
    }

    /**
     * Creates a BlogPost entity for testing purposes.
     *
     * @param id    the blog post ID
     * @param title the blog post title
     * @return a BlogPost entity
     */
    private BlogPost createBlogPost(Long id, String title) {
        return BlogPost.builder()
                .id(id)
                .title(title)
                .content("Content for " + title)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
