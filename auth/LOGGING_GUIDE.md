# Structured Logging Guide

## Table of Contents
1. [Overview](#overview)
2. [Quick Start](#quick-start)
3. [Configuration](#configuration)
4. [Using Structured Logging](#using-structured-logging)
5. [MDC Context](#mdc-context)
6. [Best Practices](#best-practices)
7. [Log Levels](#log-levels)
8. [Analyzing Logs](#analyzing-logs)
9. [Common Patterns](#common-patterns)

---

## Overview

The Auth Service uses structured JSON logging for production, making logs machine-readable and analyzable.

### Why Structured Logging?

**Traditional Logging**:
```
2026-03-01 10:30:45.123 INFO c.h.a.s.AuthenticationService : User authenticated: john.doe@example.com
```

**Structured Logging (JSON)**:
```json
{
  "timestamp": "2026-03-01T10:30:45.123Z",
  "level": "INFO",
  "logger": "com.harrish.auth.service.AuthenticationService",
  "message": "User authentication completed successfully",
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "userId": "123",
  "email": "john.doe@example.com"
}
```

**Benefits**:
- Easy to parse and analyze
- Query by specific fields
- Aggregate metrics
- Machine-readable

---

## Quick Start

### Enable JSON Logging

```bash
# Environment variable
export SPRING_PROFILES_ACTIVE=json-logs

# Or in application.properties
spring.profiles.active=json-logs
```

### Basic Usage

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
public class MyService {
    private static final Logger log = LoggerFactory.getLogger(MyService.class);
    
    public void doSomething(Long userId, String action) {
        log.info("Action performed", 
            kv("userId", userId),
            kv("action", action),
            kv("status", "success"));
    }
}
```

**Output**:
```json
{
  "timestamp": "2026-03-01T10:30:00.123Z",
  "level": "INFO",
  "message": "Action performed",
  "userId": 123,
  "action": "create_post",
  "status": "success",
  "requestId": "a1b2c3d4-..."
}
```

---

## Configuration

### logback-spring.xml

Located in `src/main/resources/logback-spring.xml`:

```xml
<configuration>
    <!-- JSON Console Appender (production) -->
    <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>requestId</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
        </encoder>
    </appender>
    
    <!-- Human-Readable Console Appender (development) -->
    <appender name="HUMAN_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- Profile: json-logs -->
    <springProfile name="json-logs">
        <root level="INFO">
            <appender-ref ref="JSON_CONSOLE"/>
        </root>
    </springProfile>
    
    <!-- Profile: default (human-readable) -->
    <springProfile name="!json-logs">
        <root level="INFO">
            <appender-ref ref="HUMAN_CONSOLE"/>
        </root>
    </springProfile>
</configuration>
```

### Profile Switching

**Development** (human-readable):
```bash
./mvnw spring-boot:run
```

**Production** (JSON):
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=json-logs
```

**Docker**:
```yaml
services:
  auth-service:
    environment:
      - SPRING_PROFILES_ACTIVE=json-logs
```

---

## Using Structured Logging

### Import Classes

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static net.logstash.logback.argument.StructuredArguments.kv;
```

### Create Logger

```java
private static final Logger log = LoggerFactory.getLogger(YourClassName.class);
```

### Log with Structured Fields

```java
log.info("User registered", 
    kv("userId", user.getId()),
    kv("email", user.getEmail()),
    kv("step", "complete"));
```

### Measure Timing

```java
long startTime = System.currentTimeMillis();
// ... perform operation ...
long duration = System.currentTimeMillis() - startTime;

log.info("Operation completed", 
    kv("operation", "createPost"),
    kv("duration_ms", duration));
```

### Log Business Events

```java
log.info("Blog post created", 
    kv("blogPostId", blogPost.getId()),
    kv("userId", user.getId()),
    kv("action", "create"));
```

### Log Errors with Context

```java
try {
    // operation
} catch (Exception ex) {
    log.error("Failed to process payment", 
        kv("userId", userId),
        kv("orderId", orderId),
        kv("errorType", ex.getClass().getSimpleName()),
        ex);
}
```

---

## MDC Context

MDC (Mapped Diagnostic Context) automatically adds fields to all logs within a request.

### How MDC Works

```
HTTP Request arrives
    ↓
RequestContextFilter
    ├─ Generate requestId
    ├─ MDC.put("requestId", requestId)
    ├─ Add X-Request-ID to response
    │
    ↓
JwtAuthenticationFilter (if authenticated)
    ├─ Extract userId from JWT
    ├─ MDC.put("userId", userId)
    │
    ↓
All logs include requestId and userId automatically
    │
    ↓
RequestContextFilter (finally block)
    └─ MDC.clear()
```

### MDC Fields

**Always Present**:
- `requestId`: Unique identifier for each request

**Present After Authentication**:
- `userId`: ID of authenticated user

### Example

```java
// In AuthenticationService
log.info("User authenticated");

// Output automatically includes MDC context:
{
  "message": "User authenticated",
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "userId": "123"
}
```

### Manual MDC Usage

```java
MDC.put("orderId", orderId.toString());
try {
    log.info("Processing order");
} finally {
    MDC.remove("orderId");
}
```

### Async Operations and MDC

MDC is thread-local. The application automatically copies context to async threads:

```java
@Async
@EventListener
public void handleEvent(SomeEvent event) {
    // MDC context automatically copied
    log.info("Event handled");  // Still has requestId, userId
}
```

---

## Best Practices

### 1. Use Consistent Field Names

**Good**:
```java
log.info("User action", kv("userId", userId), kv("action", "create"));
```

**Bad**:
```java
log.info("User action", kv("user_id", userId), kv("uid", userId));
```

### 2. Log Business Events, Not Implementation

**Good**:
```java
log.info("Blog post published", 
    kv("blogPostId", id),
    kv("userId", userId),
    kv("action", "publish"));
```

**Bad**:
```java
log.info("Method updateBlogPostStatus called");
```

### 3. Use Step Markers for Flow Tracking

```java
log.info("User registration started", kv("email", email), kv("step", "start"));
log.debug("Validation passed", kv("step", "validated"));
log.info("User registration completed", kv("userId", user.getId()), kv("step", "complete"));
```

### 4. Never Log Sensitive Data

**NEVER LOG**:
- Passwords
- JWT tokens
- Credit card numbers
- API keys

**Good**:
```java
log.info("User authenticated", kv("userId", userId));
```

**Bad**:
```java
log.info("User authenticated", kv("password", password));
```

### 5. Log at Appropriate Levels

- **ERROR**: Unrecoverable errors
- **WARN**: Recoverable issues, security events
- **INFO**: Business events
- **DEBUG**: Technical details

### 6. Include Timing for Slow Operations

```java
long startTime = System.currentTimeMillis();
// ... database query ...
long duration = System.currentTimeMillis() - startTime;

if (duration > 1000) {
    log.warn("Slow database query", 
        kv("query", "findUsersByRole"),
        kv("duration_ms", duration));
}
```

### 7. Log Once at Boundary

**Good**:
```java
@ExceptionHandler(Exception.class)
ResponseEntity<ProblemDetail> handleException(Exception ex, HttpServletRequest request) {
    log.error("Unhandled exception", 
        kv("errorType", ex.getClass().getSimpleName()),
        kv("path", request.getRequestURI()),
        ex);
}
```

**Bad** (logging at every layer):
```java
// Controller
catch (Exception ex) { log.error("Error in controller", ex); throw ex; }
// Service
catch (Exception ex) { log.error("Error in service", ex); throw ex; }
```

---

## Log Levels

### ERROR (Critical Issues)

```java
log.error("Failed to connect to database", 
    kv("errorType", "DatabaseConnectionException"),
    kv("host", dbHost),
    ex);
```

### WARN (Recoverable Issues)

```java
log.warn("Authentication failed - bad credentials", 
    kv("email", email));

log.warn("Access denied", 
    kv("userId", userId),
    kv("resource", resourceId));
```

### INFO (Business Events)

```java
log.info("User registered", kv("userId", userId));
log.info("Blog post created", kv("blogPostId", id));
```

### DEBUG (Technical Details)

```java
log.debug("JWT token validated", kv("userId", userId));
log.debug("Query executed", kv("rows", rowCount));
```

---

## Analyzing Logs

### Using jq

**Install jq**:
```bash
# Ubuntu/Debian
sudo apt-get install jq

# macOS
brew install jq
```

**Filter by field**:
```bash
# Find logs for specific user
jq 'select(.userId == "123")' logs.json

# Find ERROR logs
jq 'select(.level == "ERROR")' logs.json

# Find slow operations
jq 'select(.duration_ms > 1000)' logs.json

# Find logs for specific request
jq 'select(.requestId == "a1b2c3d4-...")' logs.json
```

**Extract fields**:
```bash
# Extract email and userId from registration logs
jq 'select(.message | contains("registered")) | {userId, email}' logs.json
```

**Calculate statistics**:
```bash
# Average duration
jq -s 'map(.duration_ms) | add / length' logs.json

# Count errors by type
jq -s 'map(select(.level == "ERROR")) | group_by(.errorType) | map({errorType: .[0].errorType, count: length})' logs.json
```

### Docker Logs with jq

```bash
# Stream and filter in real-time
docker logs -f auth-service | jq 'select(.level == "ERROR")'

# Follow logs for specific user
docker logs -f auth-service | jq 'select(.userId == "123")'
```

---

## Common Patterns

### Pattern 1: Request Lifecycle

```java
@Service
public class BlogPostService {
    public BlogPostResponse createBlogPost(CreateBlogPostRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Creating blog post", kv("step", "start"));
        
        BlogPost saved = repository.save(blogPost);
        log.debug("Blog post saved", kv("blogPostId", saved.getId()), kv("step", "saved"));
        
        eventPublisher.publishEvent(new BlogPostCreatedEvent(this, saved, user));
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("Blog post created successfully", 
            kv("blogPostId", saved.getId()),
            kv("step", "complete"),
            kv("duration_ms", duration));
        
        return mapper.toResponse(saved);
    }
}
```

### Pattern 2: Error Logging

```java
@ExceptionHandler(UserNotFoundException.class)
ResponseEntity<ProblemDetail> handleUserNotFound(
        UserNotFoundException ex, HttpServletRequest request) {
    log.warn("User not found", 
        kv("errorType", "UserNotFoundException"),
        kv("path", request.getRequestURI()),
        kv("status", 404));
    
    return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
}
```

### Pattern 3: Controller Logging

```java
@PostMapping("/blog-posts")
ResponseEntity<BlogPostResponse> createBlogPost(@Valid @RequestBody CreateBlogPostRequest request) {
    log.info("POST /api/v1/blog-posts");
    
    BlogPostResponse response = service.createBlogPost(request);
    
    log.info("Blog post created successfully", 
        kv("status", 201),
        kv("blogPostId", response.getId()));
    
    return ResponseEntity.status(201).body(response);
}
```

### Pattern 4: Trace Request

Use requestId to trace a request:

```bash
# Get all logs for specific request
jq 'select(.requestId == "a1b2c3d4-e5f6-7890")' logs.json
```

---

## Summary

### Key Takeaways

1. Enable JSON logging with `SPRING_PROFILES_ACTIVE=json-logs`
2. Use `kv()` helper for structured fields
3. MDC automatically includes requestId and userId
4. Log business events at INFO, technical details at DEBUG
5. Never log sensitive data
6. Log once at boundaries
7. Use jq for log analysis

See [ARCHITECTURE.md](ARCHITECTURE.md) for architecture details.
