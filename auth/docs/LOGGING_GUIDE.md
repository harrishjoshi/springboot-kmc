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

The Auth Service uses **structured JSON logging** for production environments, making logs machine-readable and AI-friendly. This enables powerful log analysis, monitoring, and debugging.

### Why Structured Logging?

**Traditional Logging** (Human-Readable):
```
2026-03-01 10:30:45.123 INFO  [http-nio-8080-exec-1] c.h.a.s.AuthenticationService : User authenticated: john.doe@example.com
```

**Structured Logging** (Machine-Readable JSON):
```json
{
  "timestamp": "2026-03-01T10:30:45.123Z",
  "level": "INFO",
  "logger": "com.harrish.auth.service.AuthenticationService",
  "message": "User authentication completed successfully",
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "userId": "123",
  "email": "john.doe@example.com",
  "step": "complete",
  "duration_ms": 245
}
```

**Benefits**:
- Easy to parse and analyze with tools (jq, Elasticsearch, Splunk)
- Query by specific fields (e.g., find all logs for requestId)
- Aggregate metrics (e.g., average duration_ms)
- AI-friendly for automated analysis

---

## Quick Start

### Enable JSON Logging

Set the Spring profile to `json-logs`:

```bash
# Environment variable
export SPRING_PROFILES_ACTIVE=json-logs

# Or in application.properties
spring.profiles.active=json-logs

# Or as JVM argument
java -jar app.jar --spring.profiles.active=json-logs
```

### Basic Usage in Code

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

**Output (JSON format)**:
```json
{
  "timestamp": "2026-03-01T10:30:00.123Z",
  "level": "INFO",
  "logger": "com.example.MyService",
  "message": "Action performed",
  "userId": 123,
  "action": "create_post",
  "status": "success",
  "requestId": "a1b2c3d4-...",
  "thread": "http-nio-8080-exec-1"
}
```

---

## Configuration

### logback-spring.xml

The logging configuration is in `src/main/resources/logback-spring.xml`:

```xml
<configuration>
    <!-- JSON Console Appender (production) -->
    <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <!-- Include MDC fields -->
            <includeMdcKeyName>requestId</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
            
            <!-- Customize field names -->
            <fieldNames>
                <timestamp>timestamp</timestamp>
                <message>message</message>
                <logger>logger</logger>
                <level>level</level>
            </fieldNames>
        </encoder>
    </appender>
    
    <!-- Human-Readable Console Appender (development) -->
    <appender name="HUMAN_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} [requestId=%X{requestId:-none} userId=%X{userId:-none}] - %msg%n</pattern>
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
# No profile needed, human-readable is default
./mvnw spring-boot:run
```

**Production** (JSON):
```bash
# Set json-logs profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=json-logs
```

**Docker**:
```yaml
# docker-compose.yml
services:
  auth-service:
    environment:
      - SPRING_PROFILES_ACTIVE=json-logs
```

---

## Using Structured Logging

### Import Required Classes

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static net.logstash.logback.argument.StructuredArguments.kv;
```

### Create Logger Instance

```java
private static final Logger log = LoggerFactory.getLogger(YourClassName.class);
```

### Log with Structured Fields

Use `kv("key", value)` to add structured fields:

```java
log.info("User registered", 
    kv("userId", user.getId()),
    kv("email", user.getEmail()),
    kv("step", "complete"));
```

**Output**:
```json
{
  "message": "User registered",
  "userId": 123,
  "email": "user@example.com",
  "step": "complete",
  "requestId": "...",
  "timestamp": "..."
}
```

### Measure Timing

Track operation duration:

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
    kv("title", blogPost.getTitle()),
    kv("action", "create"),
    kv("step", "complete"));
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
        ex);  // Include stack trace
}
```

---

## MDC Context

**MDC (Mapped Diagnostic Context)** automatically adds fields to all logs within a request.

### How MDC Works

```
HTTP Request arrives
    ↓
RequestContextFilter
    ├─ Generate requestId (UUID or from X-Request-ID header)
    ├─ MDC.put("requestId", requestId)
    ├─ Add X-Request-ID to response header
    │
    ↓
JwtAuthenticationFilter (if authenticated)
    ├─ Extract userId from JWT
    ├─ MDC.put("userId", userId)
    │
    ↓
All subsequent logs include requestId and userId automatically
    │
    ↓
RequestContextFilter (finally block)
    └─ MDC.clear()  // Cleanup to prevent memory leaks
```

### MDC Fields

**Always Present**:
- `requestId`: Unique identifier for each HTTP request

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
  "userId": "123",  // Added by JwtAuthenticationFilter
  "timestamp": "..."
}
```

### Manual MDC Usage

Add custom MDC fields (rare, usually automatic):

```java
MDC.put("orderId", orderId.toString());
try {
    // All logs here include orderId
    log.info("Processing order");
} finally {
    MDC.remove("orderId");  // Cleanup
}
```

### Async Operations and MDC

MDC is thread-local, so it doesn't automatically propagate to async threads. The application handles this automatically:

```java
@Async
@EventListener
public void handleEvent(SomeEvent event) {
    // MDC context is automatically copied to this thread
    // by TaskExecutor configuration
    log.info("Event handled");  // Still has requestId, userId
}
```

---

## Best Practices

### 1. Use Consistent Field Names

**Good**:
```java
log.info("User action", kv("userId", userId), kv("action", "create"));
log.info("Order placed", kv("userId", userId), kv("orderId", orderId));
```

**Bad** (inconsistent naming):
```java
log.info("User action", kv("user_id", userId), kv("action", "create"));
log.info("Order placed", kv("uid", userId), kv("order_id", orderId));
```

### 2. Log Business Events, Not Implementation Details

**Good** (business context):
```java
log.info("Blog post published", 
    kv("blogPostId", id),
    kv("userId", userId),
    kv("category", category),
    kv("action", "publish"));
```

**Bad** (technical details):
```java
log.info("Method updateBlogPostStatus called with id=" + id);
```

### 3. Use Step Markers for Flow Tracking

```java
log.info("User registration started", kv("email", email), kv("step", "start"));
// ... validation ...
log.debug("Validation passed", kv("step", "validated"));
// ... save user ...
log.debug("User saved", kv("userId", user.getId()), kv("step", "saved"));
// ... publish event ...
log.info("User registration completed", kv("userId", user.getId()), kv("step", "complete"));
```

Query all logs for a specific flow:
```bash
jq 'select(.email == "user@example.com")' logs.json
```

### 4. Never Log Sensitive Data

**NEVER LOG**:
- Passwords (plaintext or hashed)
- JWT tokens (access or refresh)
- Credit card numbers
- Social security numbers
- API keys or secrets

**Good**:
```java
log.info("User authenticated", kv("userId", userId), kv("email", email));
```

**Bad**:
```java
log.info("User authenticated", kv("password", password), kv("token", jwt));
```

### 5. Log at Appropriate Levels

- **ERROR**: Unrecoverable errors, service failures
- **WARN**: Recoverable issues, security events (bad credentials, access denied)
- **INFO**: Business events (registration, login, CRUD operations)
- **DEBUG**: Technical details, flow steps, SQL queries

### 6. Include Timing for Slow Operations

```java
long startTime = System.currentTimeMillis();
// ... database query ...
long duration = System.currentTimeMillis() - startTime;

if (duration > 1000) {  // Log if > 1 second
    log.warn("Slow database query", 
        kv("query", "findUsersByRole"),
        kv("duration_ms", duration));
}
```

### 7. Log Once at Boundary

**Good** (log once in GlobalExceptionHandler):
```java
@ExceptionHandler(Exception.class)
ResponseEntity<ProblemDetail> handleException(Exception ex, HttpServletRequest request) {
    log.error("Unhandled exception", 
        kv("errorType", ex.getClass().getSimpleName()),
        kv("path", request.getRequestURI()),
        kv("method", request.getMethod()),
        ex);
    // ...
}
```

**Bad** (log at every layer):
```java
// Controller
catch (Exception ex) { log.error("Error in controller", ex); throw ex; }
// Service
catch (Exception ex) { log.error("Error in service", ex); throw ex; }
// Repository
catch (Exception ex) { log.error("Error in repository", ex); throw ex; }
```

---

## Log Levels

### ERROR (Critical Issues)

Use for unhandled exceptions and service failures:

```java
log.error("Failed to connect to database", 
    kv("errorType", "DatabaseConnectionException"),
    kv("host", dbHost),
    ex);
```

### WARN (Recoverable Issues)

Use for handled exceptions and security events:

```java
log.warn("Authentication failed - bad credentials", 
    kv("email", email),
    kv("attemptCount", attemptCount));

log.warn("Access denied", 
    kv("userId", userId),
    kv("resource", resourceId),
    kv("requiredRole", "ADMIN"));
```

### INFO (Business Events)

Use for important business operations:

```java
log.info("User registered", kv("userId", userId), kv("email", email));
log.info("Blog post created", kv("blogPostId", id), kv("userId", userId));
log.info("Password changed", kv("userId", userId));
```

### DEBUG (Technical Details)

Use for debugging and development:

```java
log.debug("JWT token validated", kv("userId", userId), kv("expiresIn", expiresIn));
log.debug("Query executed", kv("query", sql), kv("rows", rowCount));
```

---

## Analyzing Logs

### Using jq (Command Line)

**Install jq**:
```bash
# Ubuntu/Debian
sudo apt-get install jq

# macOS
brew install jq
```

**Filter by field**:
```bash
# Find all logs for specific user
jq 'select(.userId == "123")' logs.json

# Find all ERROR logs
jq 'select(.level == "ERROR")' logs.json

# Find slow operations (> 1 second)
jq 'select(.duration_ms > 1000)' logs.json

# Find logs for specific request
jq 'select(.requestId == "a1b2c3d4-...")' logs.json
```

**Extract specific fields**:
```bash
# Extract email and userId from registration logs
jq 'select(.message | contains("registered")) | {userId, email}' logs.json
```

**Calculate statistics**:
```bash
# Average duration for authentication
jq -s 'map(select(.message | contains("authentication"))) | map(.duration_ms) | add / length' logs.json

# Count errors by type
jq -s 'map(select(.level == "ERROR")) | group_by(.errorType) | map({errorType: .[0].errorType, count: length})' logs.json
```

### Docker Logs with jq

```bash
# Stream logs and filter in real-time
docker logs -f auth-service | jq 'select(.level == "ERROR")'

# Follow logs for specific user
docker logs -f auth-service | jq 'select(.userId == "123")'
```

### Elasticsearch / Kibana

Send logs to Elasticsearch for powerful querying and visualization:

```json
POST /logs-auth/_search
{
  "query": {
    "bool": {
      "must": [
        {"term": {"userId": "123"}},
        {"range": {"duration_ms": {"gte": 1000}}}
      ]
    }
  }
}
```

---

## Common Patterns

### Pattern 1: Request Lifecycle Logging

```java
@Service
public class BlogPostService {
    public BlogPostResponse createBlogPost(CreateBlogPostRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Creating blog post", kv("step", "start"));
        
        // Validation
        log.debug("Validating input", kv("step", "validate"));
        
        // Save
        BlogPost saved = repository.save(blogPost);
        log.debug("Blog post saved", kv("blogPostId", saved.getId()), kv("step", "saved"));
        
        // Publish event
        eventPublisher.publishEvent(new BlogPostCreatedEvent(this, saved, user));
        log.debug("Event published", kv("step", "event_published"));
        
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
        kv("method", request.getMethod()),
        kv("requestId", MDC.get("requestId")),
        kv("status", 404));
    
    return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
}
```

### Pattern 3: Controller Logging

```java
@PostMapping("/blog-posts")
ResponseEntity<BlogPostResponse> createBlogPost(@Valid @RequestBody CreateBlogPostRequest request) {
    log.info("POST /api/v1/blog-posts", 
        kv("method", "POST"),
        kv("path", "/api/v1/blog-posts"));
    
    BlogPostResponse response = service.createBlogPost(request);
    
    log.info("Blog post created successfully", 
        kv("path", "/api/v1/blog-posts"),
        kv("status", 201),
        kv("blogPostId", response.getId()));
    
    return ResponseEntity.status(201).body(response);
}
```

### Pattern 4: Trace Request Across Services

Use requestId to trace a request through the entire system:

```bash
# Get all logs for a specific request
jq 'select(.requestId == "a1b2c3d4-e5f6-7890")' logs.json

# Output shows complete request flow:
# 1. Request received at controller
# 2. Service method called
# 3. Database query executed
# 4. Event published
# 5. Response returned
```

---

## Summary

### Key Takeaways

1. **Enable JSON logging** in production with `SPRING_PROFILES_ACTIVE=json-logs`
2. **Use `kv()` helper** for structured fields: `kv("userId", userId)`
3. **MDC context** automatically includes `requestId` and `userId` in all logs
4. **Log business events** at INFO level, technical details at DEBUG
5. **Never log sensitive data** (passwords, tokens, PII)
6. **Log once at boundaries** (GlobalExceptionHandler, not every layer)
7. **Use jq or Elasticsearch** to analyze logs efficiently

### Next Steps

- Review examples in the codebase:
  - `AuthenticationService`: User registration/login logging
  - `BlogPostService`: CRUD operation logging
  - `GlobalExceptionHandler`: Error logging with context
- Experiment with jq queries on production logs
- Set up log aggregation (Elasticsearch/Kibana or similar)
- Create dashboards for key metrics (error rates, slow queries, user activity)

For architecture details, see [ARCHITECTURE.md](ARCHITECTURE.md).
