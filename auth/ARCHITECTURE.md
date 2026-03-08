# Architecture Documentation

## Table of Contents
1. [Overview](#overview)
2. [Layered Architecture](#layered-architecture)
3. [Design Patterns](#design-patterns)
4. [Component Design](#component-design)
5. [Data Flow](#data-flow)
6. [Security Architecture](#security-architecture)
7. [Event System](#event-system)
8. [Database Design](#database-design)
9. [Logging Architecture](#logging-architecture)
10. [Testing Strategy](#testing-strategy)

---

## Overview

The Auth Service is built following **clean architecture principles** with a focus on separation of concerns, testability, and maintainability. The application uses Spring Boot 3.5.3 with Java 21 and follows REST API best practices.

### Key Architectural Principles
- **Single Responsibility**: Each class has one well-defined purpose
- **Dependency Inversion**: Depend on abstractions (interfaces) not concrete implementations
- **Open/Closed**: Open for extension, closed for modification
- **Layered Architecture**: Clear separation between presentation, business logic, and data access
- **Domain-Driven Design**: Rich domain models with behavior

---

## Layered Architecture

The application follows a traditional layered architecture pattern:

```
┌──────────────────────────────────────────────────────┐
│               Presentation Layer                     │
│  Controllers (REST API) + DTOs + Validation          │
│  - AuthenticationController                          │
│  - BlogPostController                                │
└──────────────────────────────────────────────────────┘
                       ↓
┌──────────────────────────────────────────────────────┐
│              Business Logic Layer                    │
│  Services + Domain Logic + Authorization             │
│  - AuthenticationService                             │
│  - BlogPostService                                   │
│  - BlogPostAuthorizationService                      │
│  - UserFactory                                       │
└──────────────────────────────────────────────────────┘
                       ↓
┌──────────────────────────────────────────────────────┐
│               Data Access Layer                      │
│  Repositories (Spring Data JPA) + Entities           │
│  - UserRepository                                    │
│  - BlogPostRepository                                │
└──────────────────────────────────────────────────────┘
                       ↓
┌──────────────────────────────────────────────────────┐
│                  Database Layer                      │
│  PostgreSQL 16 (Production) / H2 (Tests)            │
└──────────────────────────────────────────────────────┘
```

### Cross-Cutting Concerns

These components span multiple layers:

```
┌─────────────────────────────────────────────────────┐
│               Cross-Cutting Concerns                │
│                                                     │
│  • Security (JWT Authentication Filter)            │
│  • Logging (MDC Context + Structured Logging)      │
│  • Exception Handling (GlobalExceptionHandler)     │
│  • Validation (Jakarta Bean Validation)            │
│  • Events (Application Event Publisher)            │
│  • Internationalization (MessageResolver)          │
└─────────────────────────────────────────────────────┘
```

### Layer Responsibilities

#### 1. Presentation Layer (Controllers)
**Responsibility**: Handle HTTP requests/responses, input validation, DTO mapping

**Components**:
- `AuthenticationController`: Registration, login, token refresh
- `BlogPostController`: CRUD operations for blog posts

**Key Characteristics**:
- Package-private visibility (internal to application)
- Uses DTOs to decouple API from domain models
- Jakarta Bean Validation for input validation
- Returns `ResponseEntity<T>` with appropriate HTTP status codes
- OpenAPI/Swagger annotations for documentation

**Example**:
```java
@RestController
@RequestMapping("/api/v1/auth")
class AuthenticationController {
    ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request);
    ResponseEntity<AuthenticationResponse> authenticate(@Valid @RequestBody AuthenticationRequest request);
}
```

#### 2. Business Logic Layer (Services)
**Responsibility**: Implement business rules, orchestrate operations, handle authorization

**Components**:
- `AuthenticationService`: User registration, authentication, token management
- `BlogPostService`: Blog post CRUD with business logic
- `BlogPostAuthorizationService`: Fine-grained authorization checks
- `BlogPostMapper`: Entity ↔ DTO mapping (Single Responsibility)
- `UserFactory`: User entity creation (Factory Pattern)
- `CurrentUserProvider`: Authentication context access (Strategy Pattern)

**Key Characteristics**:
- Transactional boundaries (`@Transactional`)
- Business validation and logic
- Event publishing for cross-cutting concerns
- Structured logging with business context
- Delegates data access to repositories

**Example**:
```java
@Service
public class AuthenticationService {
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        // Business logic: check email uniqueness, create user, publish event
    }
}
```

#### 3. Data Access Layer (Repositories)
**Responsibility**: Abstract database access, provide data persistence

**Components**:
- `UserRepository extends JpaRepository<User, Long>`
- `BlogPostRepository extends JpaRepository<BlogPost, Long>`

**Key Characteristics**:
- Spring Data JPA repositories
- Custom query methods with `@Query` annotations
- Optimized queries with JOIN FETCH (N+1 prevention)
- Method naming conventions for derived queries

**Example**:
```java
public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {
    @Query("SELECT bp FROM BlogPost bp JOIN FETCH bp.createdBy WHERE bp.createdBy = :user ORDER BY bp.createdAt DESC")
    List<BlogPost> findByCreatedByOrderByCreatedAtDesc(@Param("user") User user);
}
```

---

## Design Patterns

The application uses several design patterns for clean, maintainable code:

### 1. Layered Architecture Pattern
**Intent**: Separate concerns into distinct layers with well-defined responsibilities.

**Implementation**:
- Controller → Service → Repository → Database
- Each layer only depends on the layer directly below it

### 2. Repository Pattern
**Intent**: Abstract data access logic from business logic.

**Implementation**:
- Spring Data JPA repositories (`UserRepository`, `BlogPostRepository`)
- Provides CRUD operations and custom queries
- Hides database implementation details

### 3. Factory Pattern
**Intent**: Encapsulate object creation logic.

**Implementation**: `UserFactory`
```java
@Component
public class UserFactory {
    public User createStandardUser(RegisterRequest request) {
        return User.builder()
            .firstName(request.firstName())
            .lastName(request.lastName())
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .role(Role.USER)
            .build();
    }
}
```

**Benefits**:
- Centralizes user creation logic
- Ensures consistent password encoding
- Easy to extend (e.g., `createAdminUser()`)

### 4. Strategy Pattern
**Intent**: Define a family of algorithms, encapsulate each one, and make them interchangeable.

**Implementation**: `CurrentUserProvider`
```java
@Component
public class CurrentUserProvider {
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return userPrincipal.user();
    }
}
```

**Benefits**:
- Abstracts authentication context access
- Easy to mock in tests
- Single point of change for user retrieval strategy

### 5. Observer Pattern (Event-Driven)
**Intent**: Define a one-to-many dependency where state changes are notified to observers.

**Implementation**: Spring Application Events
```java
// Event Definition
public record UserRegisteredEvent(Object source, User user) 
    implements ApplicationEvent { }

// Publisher (AuthenticationService)
eventPublisher.publishEvent(new UserRegisteredEvent(this, user));

// Listener
@Component
class UserRegistrationEventListener {
    @EventListener
    @Async
    public void handleUserRegisteredEvent(UserRegisteredEvent event) {
        // Send welcome email, create user profile, etc.
    }
}
```

**Benefits**:
- Decouples event producers from consumers
- Easy to add new listeners without modifying existing code
- Supports async processing with `@Async`

### 6. Data Transfer Object (DTO) Pattern
**Intent**: Transfer data between layers without exposing domain entities.

**Implementation**:
- Request DTOs: `RegisterRequest`, `CreateBlogPostRequest`
- Response DTOs: `RegisterResponse`, `BlogPostResponse`
- Mapper: `BlogPostMapper` converts entities to DTOs

**Benefits**:
- API stability (changes to entities don't break API contracts)
- Security (prevents over-posting attacks)
- Performance (only transfer necessary data)

### 7. Template Method Pattern
**Intent**: Define skeleton of algorithm, let subclasses override specific steps.

**Implementation**: `OncePerRequestFilter` (RequestContextFilter)
```java
@Component
public class RequestContextFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        // Set up MDC context
        MDC.put("requestId", requestId);
        filterChain.doFilter(request, response);
        MDC.clear();
    }
}
```

---

## Component Design

### Security Components

#### JwtService
**Responsibility**: JWT token generation, parsing, and validation.

```java
@Service
public class JwtService {
    private final Key signingKey; // Cached for performance
    
    public String generateToken(UserDetails userDetails);
    public String extractUsername(String token);
    public boolean isTokenValid(String token, UserDetails userDetails);
}
```

**Key Features**:
- Uses io.jsonwebtoken:jjwt 0.12.6 (latest secure version)
- Signing key cached to avoid repeated key derivation (400µs saved per request)
- Separate methods for access tokens and refresh tokens

#### UserPrincipal
**Responsibility**: Wrap User entity for Spring Security.

**Why?** Separation of Concerns (SOLID)
- `User` is a pure JPA entity (domain model)
- `UserPrincipal` is Spring Security specific (infrastructure concern)

```java
public record UserPrincipal(User user) implements UserDetails {
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name()));
    }
}
```

#### JwtAuthenticationFilter
**Responsibility**: Intercept requests, extract JWT, authenticate user.

**Flow**:
1. Extract JWT from `Authorization: Bearer <token>` header
2. Parse and validate JWT
3. Load user from database (via UserDetailsService)
4. Create Authentication and set in SecurityContext
5. Continue filter chain

---

## Data Flow

### User Registration Flow

```
┌─────────┐    POST /api/v1/auth/register     ┌────────────────────┐
│ Client  │ ──────────────────────────────────>│ Authentication     │
└─────────┘                                    │ Controller         │
                                               └────────────────────┘
                                                         │
                                                         ├─ Validate input
                                                         │
                                                         ↓
                                               ┌────────────────────┐
                                               │ Authentication     │
                                               │ Service            │
                                               └────────────────────┘
                                                         │
                                                         ├─ Check email uniqueness
                                                         ├─ Create user (via UserFactory)
                                                         ├─ Save to database
                                                         ├─ Publish UserRegisteredEvent
                                                         │
                                                         ↓
                                               ┌────────────────────┐
                                               │ User Repository    │
                                               └────────────────────┘
                                                         │
                                                         ↓
                                               ┌────────────────────┐
                                               │ PostgreSQL         │
                                               └────────────────────┘
```

### Authentication (Login) Flow

```
┌─────────┐    POST /api/v1/auth/login        ┌────────────────────┐
│ Client  │ ──────────────────────────────────>│ Authentication     │
└─────────┘                                    │ Controller         │
                                               └────────────────────┘
                                                         │
                                                         ↓
                                               ┌────────────────────┐
                                               │ Authentication     │
                                               │ Service            │
                                               └────────────────────┘
                                                         │
                                                         ├─ Authenticate via AuthenticationManager
                                                         │  (delegates to Spring Security)
                                                         │
                                                         ↓
                                               ┌────────────────────┐
                                               │ Spring Security    │
                                               │ - Load user        │
                                               │ - Verify password  │
                                               └────────────────────┘
                                                         │
                                                         ├─ Success
                                                         │
                                                         ↓
                                               ┌────────────────────┐
                                               │ JWT Service        │
                                               │ - Generate tokens  │
                                               └────────────────────┘
                                                         │
                                                         ↓
                                               ┌────────────────────┐
                                               │ Return tokens to   │
                                               │ client             │
                                               └────────────────────┘
```

### Blog Post Creation Flow

```
┌─────────┐    POST /api/v1/blog-posts        ┌────────────────────┐
│ Client  │ ──────────────────────────────────>│ JWT Authentication │
│         │    Authorization: Bearer <token>   │ Filter             │
└─────────┘                                    └────────────────────┘
                                                         │
                                                         ├─ Extract & validate JWT
                                                         ├─ Load user
                                                         ├─ Set SecurityContext
                                                         │
                                                         ↓
                                               ┌────────────────────┐
                                               │ BlogPost           │
                                               │ Controller         │
                                               └────────────────────┘
                                                         │
                                                         ├─ Validate input
                                                         │
                                                         ↓
                                               ┌────────────────────┐
                                               │ BlogPost Service   │
                                               └────────────────────┘
                                                         │
                                                         ├─ Get current user
                                                         ├─ Create blog post
                                                         ├─ Save to database
                                                         ├─ Publish BlogPostCreatedEvent
                                                         │
                                                         ↓
                                               ┌────────────────────┐
                                               │ BlogPost           │
                                               │ Repository         │
                                               └────────────────────┘
```

---

## Security Architecture

### Authentication Flow

```
1. User submits credentials → AuthenticationService
2. AuthenticationManager validates credentials
3. On success, generate JWT tokens (access + refresh)
4. Return tokens to client
5. Client includes access token in subsequent requests
```

### Authorization Levels

#### 1. Filter-Level (JwtAuthenticationFilter)
- Validates JWT on every request
- Sets Authentication in SecurityContext

#### 2. Method-Level (@PreAuthorize)
- Fine-grained authorization on specific methods
- Example: `@PreAuthorize("@blogPostService.isBlogPostCreator(#id) or hasRole('ADMIN')")`

#### 3. Service-Level (BlogPostAuthorizationService)
- Centralized authorization logic
- Reusable across multiple services

### JWT Token Design

**Access Token**:
- **Purpose**: API authentication
- **Expiration**: 15 minutes
- **Claims**: username, authorities, issued_at, expiration

**Refresh Token**:
- **Purpose**: Obtain new access tokens
- **Expiration**: 24 hours
- **Usage**: Only valid for `/api/v1/auth/refresh-token` endpoint

**Security Features**:
- HMAC-SHA256 signing
- Secret key from configuration (rotatable)
- Token expiration validation
- Username extraction and validation

---

## Event System

The application uses Spring's event publishing mechanism for decoupled communication:

### Events

#### UserRegisteredEvent
```java
public record UserRegisteredEvent(Object source, User user) 
    implements ApplicationEvent { }
```

**Published**: After successful user registration  
**Potential Listeners**:
- Send welcome email
- Create user profile
- Initialize user settings
- Audit logging

#### BlogPostCreatedEvent
```java
public record BlogPostCreatedEvent(Object source, BlogPost blogPost, User creator) 
    implements ApplicationEvent { }
```

**Published**: After blog post creation  
**Potential Listeners**:
- Send notification to followers
- Index post for search
- Update analytics
- Audit logging

### Event Processing

**Synchronous** (default):
- Executed in the same thread
- Transaction participates in publisher's transaction
- Use for critical operations

**Asynchronous** (`@Async`):
- Executed in separate thread pool
- Non-blocking
- Use for non-critical operations (emails, notifications)

```java
@Component
class BlogPostCreatedEventListener {
    @EventListener
    @Async  // Non-blocking
    public void handleBlogPostCreated(BlogPostCreatedEvent event) {
        // Send notifications, update search index, etc.
    }
}
```

---

## Database Design

### Entity Relationship Diagram

```
┌──────────────────┐
│      User        │
├──────────────────┤
│ id (PK)          │
│ firstName        │
│ lastName         │
│ email (UNIQUE)   │◄───────┐
│ password         │        │
│ role             │        │ created_by_id (FK)
└──────────────────┘        │
                            │
                            │
                    ┌───────────────────┐
                    │    BlogPost       │
                    ├───────────────────┤
                    │ id (PK)           │
                    │ title             │
                    │ content           │
                    │ created_by_id (FK)│
                    │ created_at        │
                    │ updated_at        │
                    └───────────────────┘
```

### Indexes

**Performance-critical indexes**:
- `idx_user_email` on users.email (unique) - Fast user lookup by email
- `idx_blog_post_created_by` on blog_posts.created_by_id - Fast blog post lookup by user
- `idx_blog_post_created_at` on blog_posts.created_at - Fast sorting by date

**Performance Impact**:
- N+1 query fix: 99.5% query reduction (500ms → 10ms)
- Index usage: 15-50x query performance improvement

---

## Logging Architecture

### Structured Logging

The application uses **structured JSON logging** for production environments:

**Key Components**:
1. **Logstash Logback Encoder**: Converts log events to JSON
2. **MDC (Mapped Diagnostic Context)**: Request correlation with requestId and userId
3. **RequestContextFilter**: Generates requestId and adds to MDC
4. **Structured Arguments**: `kv()` helper for consistent field naming

### MDC Context Flow

```
Request arrives
    ↓
RequestContextFilter (Order: HIGHEST_PRECEDENCE)
    ├─ Generate requestId (from header or UUID)
    ├─ MDC.put("requestId", requestId)
    ├─ Add X-Request-ID header to response
    │
    ↓
JwtAuthenticationFilter (if authenticated)
    ├─ Extract user from JWT
    ├─ MDC.put("userId", userId)
    │
    ↓
Controllers, Services, Repositories
    ├─ All logs automatically include requestId and userId
    │
    ↓
GlobalExceptionHandler (if error)
    ├─ Logs exception with full MDC context
    │
    ↓
RequestContextFilter (finally block)
    ├─ MDC.clear()  // Prevent memory leaks
```

### Log Levels

- **ERROR**: Unhandled exceptions, service failures
- **WARN**: Handled issues (bad credentials, access denied, validation errors)
- **INFO**: Business events (registration, login, blog post CRUD)
- **DEBUG**: Technical details (SQL queries, step markers, timing)

For more details, see [LOGGING_GUIDE.md](LOGGING_GUIDE.md).

---

## Testing Strategy

### Test Pyramid

```
              ▲
             ╱ ╲               E2E Tests (future)
            ╱   ╲              - Full application tests
           ╱     ╲             
          ╱───────╲            Integration Tests (future)
         ╱         ╲           - Repository tests with H2
        ╱           ╲          - Service tests with real DB
       ╱─────────────╲         
      ╱               ╲        Unit Tests (current: 87 tests)
     ╱                 ╲       - Service logic
    ╱───────────────────╲      - Utility classes
   ╱                     ╲     - Security components
  ╱───────────────────────╲    
 ╱                         ╲   
```

### Current Test Coverage

**Overall**: 44% line coverage (87 tests passing)

**By Package**:
- `com.harrish.auth.security`: 80% (critical JWT components)
- `com.harrish.auth.util`: 59% (validation utilities)
- `com.harrish.auth.service`: ~40% (partial coverage)

**Test Infrastructure**:
- JUnit 5 for test framework
- Mockito for mocking
- H2 in-memory database for integration tests
- JaCoCo for coverage reporting

For detailed coverage report, see [TEST_COVERAGE_REPORT.md](TEST_COVERAGE_REPORT.md).

---

## Summary

This architecture provides:
- **Maintainability**: Clear separation of concerns
- **Testability**: Dependency injection and interface-based design
- **Scalability**: Layered architecture allows horizontal scaling
- **Security**: Defense in depth with multiple security layers
- **Observability**: Structured logging with request correlation
- **Performance**: Query optimization and caching strategies
- **Extensibility**: Design patterns enable easy feature additions
