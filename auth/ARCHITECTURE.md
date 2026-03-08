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

---

## Overview

The Auth Service follows clean architecture principles with separation of concerns, testability, and maintainability. Built with Spring Boot 3.5.3 and Java 21.

### Key Principles
- **Single Responsibility**: Each class has one purpose
- **Dependency Inversion**: Depend on abstractions
- **Open/Closed**: Open for extension, closed for modification
- **Layered Architecture**: Clear layer separation
- **Domain-Driven Design**: Rich domain models

---

## Layered Architecture

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
**Responsibility**: HTTP requests/responses, input validation, DTO mapping

**Components**:
- `AuthenticationController`: Registration, login, token refresh
- `BlogPostController`: CRUD operations

**Key Characteristics**:
- Package-private visibility
- DTOs to decouple API from domain
- Jakarta Bean Validation
- Returns `ResponseEntity<T>`
- OpenAPI annotations

#### 2. Business Logic Layer (Services)
**Responsibility**: Business rules, orchestration, authorization

**Components**:
- `AuthenticationService`: User registration, authentication, token management
- `BlogPostService`: Blog post CRUD
- `BlogPostAuthorizationService`: Authorization checks
- `BlogPostMapper`: Entity ↔ DTO mapping
- `UserFactory`: User entity creation
- `CurrentUserProvider`: Authentication context access

**Key Characteristics**:
- Transactional boundaries
- Business validation
- Event publishing
- Structured logging
- Delegates data access to repositories

#### 3. Data Access Layer (Repositories)
**Responsibility**: Database access, data persistence

**Components**:
- `UserRepository extends JpaRepository<User, Long>`
- `BlogPostRepository extends JpaRepository<BlogPost, Long>`

**Key Characteristics**:
- Spring Data JPA
- Custom queries with @Query
- Optimized queries with JOIN FETCH
- Derived query methods

---

## Design Patterns

### 1. Layered Architecture Pattern
**Intent**: Separate concerns into layers with defined responsibilities.

**Implementation**: Controller → Service → Repository → Database

### 2. Repository Pattern
**Intent**: Abstract data access from business logic.

**Implementation**: Spring Data JPA repositories (UserRepository, BlogPostRepository)

### 3. Factory Pattern
**Intent**: Encapsulate object creation.

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

### 4. Strategy Pattern
**Intent**: Make algorithms interchangeable.

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

### 5. Observer Pattern (Event-Driven)
**Intent**: Define one-to-many dependency for state changes.

**Implementation**: Spring Application Events
```java
// Event Definition
public record UserRegisteredEvent(Object source, User user) 
    implements ApplicationEvent { }

// Publisher
eventPublisher.publishEvent(new UserRegisteredEvent(this, user));

// Listener
@Component
class UserRegistrationEventListener {
    @EventListener
    @Async
    public void handleUserRegisteredEvent(UserRegisteredEvent event) {
        // Send welcome email, etc.
    }
}
```

### 6. Data Transfer Object (DTO) Pattern
**Intent**: Transfer data between layers without exposing entities.

**Implementation**:
- Request DTOs: `RegisterRequest`, `CreateBlogPostRequest`
- Response DTOs: `RegisterResponse`, `BlogPostResponse`
- Mapper: `BlogPostMapper`

### 7. Template Method Pattern
**Intent**: Define algorithm skeleton, let subclasses override steps.

**Implementation**: `OncePerRequestFilter` (RequestContextFilter)
```java
@Component
public class RequestContextFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
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
**Responsibility**: JWT generation, parsing, validation.

**Key Features**:
- Uses io.jsonwebtoken:jjwt 0.12.6
- Cached signing key for performance
- Separate methods for access and refresh tokens

#### UserPrincipal
**Responsibility**: Wrap User entity for Spring Security.

**Why?** Separation of concerns - User is pure JPA entity, UserPrincipal is Spring Security specific.

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
1. Extract JWT from Authorization header
2. Parse and validate JWT
3. Load user from database
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
                                                         ├─ Create user (UserFactory)
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
                                               │ Return tokens      │
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
3. On success, generate JWT tokens
4. Return tokens to client
5. Client includes access token in requests
```

### Authorization Levels

#### 1. Filter-Level (JwtAuthenticationFilter)
- Validates JWT on every request
- Sets Authentication in SecurityContext

#### 2. Method-Level (@PreAuthorize)
- Fine-grained authorization
- Example: `@PreAuthorize("@blogPostService.isBlogPostCreator(#id) or hasRole('ADMIN')")`

#### 3. Service-Level (BlogPostAuthorizationService)
- Centralized authorization logic
- Reusable across services

### JWT Token Design

**Access Token**:
- **Purpose**: API authentication
- **Expiration**: 15 minutes
- **Claims**: username, authorities, issued_at, expiration

**Refresh Token**:
- **Purpose**: Obtain new access tokens
- **Expiration**: 24 hours
- **Usage**: Only for /api/v1/auth/refresh-token

**Security Features**:
- HMAC-SHA256 signing
- Configurable secret key
- Token expiration validation
- Username extraction and validation

---

## Event System

### Events

#### UserRegisteredEvent
```java
public record UserRegisteredEvent(Object source, User user) 
    implements ApplicationEvent { }
```

**Published**: After successful registration  
**Potential Listeners**: Welcome email, user profile creation, audit logging

#### BlogPostCreatedEvent
```java
public record BlogPostCreatedEvent(Object source, BlogPost blogPost, User creator) 
    implements ApplicationEvent { }
```

**Published**: After blog post creation  
**Potential Listeners**: Notifications, search indexing, analytics

### Event Processing

**Synchronous** (default):
- Same thread
- Participates in publisher's transaction
- Use for critical operations

**Asynchronous** (@Async):
- Separate thread pool
- Non-blocking
- Use for non-critical operations

```java
@Component
class BlogPostCreatedEventListener {
    @EventListener
    @Async
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
- `idx_user_email` on users.email (unique)
- `idx_blog_post_created_by` on blog_posts.created_by_id
- `idx_blog_post_created_at` on blog_posts.created_at

---

## Logging Architecture

### Structured Logging

Uses structured JSON logging for production environments.

**Key Components**:
1. **Logstash Logback Encoder**: Converts logs to JSON
2. **MDC**: Request correlation with requestId and userId
3. **RequestContextFilter**: Generates requestId
4. **Structured Arguments**: `kv()` helper for field naming

### MDC Context Flow

```
Request arrives
    ↓
RequestContextFilter (Order: HIGHEST_PRECEDENCE)
    ├─ Generate requestId
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
    ├─ All logs include requestId and userId
    │
    ↓
GlobalExceptionHandler (if error)
    ├─ Logs exception with MDC context
    │
    ↓
RequestContextFilter (finally block)
    ├─ MDC.clear()
```

### Log Levels

- **ERROR**: Unhandled exceptions, service failures
- **WARN**: Handled issues (bad credentials, access denied)
- **INFO**: Business events (registration, login, CRUD)
- **DEBUG**: Technical details (SQL queries, timing)

See [LOGGING_GUIDE.md](LOGGING_GUIDE.md) for details.

---

## Summary

This architecture provides:
- **Maintainability**: Clear separation of concerns
- **Testability**: Dependency injection and interfaces
- **Scalability**: Layered architecture for horizontal scaling
- **Security**: Defense in depth
- **Observability**: Structured logging with request correlation
- **Performance**: Query optimization and caching
- **Extensibility**: Design patterns enable easy additions
