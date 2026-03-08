# Auth Service

A Spring Boot application that provides authentication and blog post management functionality with enterprise-grade security, structured logging, and comprehensive test coverage.

## Features

- User registration and authentication with JWT
- Token refresh mechanism with secure validation
- Blog post CRUD operations with fine-grained authorization
- RESTful API design following best practices
- Comprehensive error handling with RFC 7807 Problem Details
- Internationalization support (i18n)
- Structured JSON logging with request correlation (MDC)
- Event-driven architecture with Observer pattern
- Repository layer with optimized queries (N+1 prevention)
- Comprehensive test coverage with strategic focus on security components
- Database indexing for performance optimization

## Architecture

### High-Level Overview

The application follows a layered architecture pattern with clear separation of concerns:

```
┌─────────────────────────────────────────────────────┐
│                   Controllers                       │
│          (REST API Endpoints + Validation)          │
└─────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────┐
│                     Services                        │
│          (Business Logic + Authorization)           │
└─────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────┐
│                   Repositories                      │
│          (Data Access Layer - JPA/Hibernate)        │
└─────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────┐
│                  PostgreSQL 16                      │
│              (Relational Database)                  │
└─────────────────────────────────────────────────────┘
```

**Cross-Cutting Concerns:**
- **Security**: JWT authentication filter, Spring Security configuration
- **Logging**: Structured JSON logging with MDC (requestId, userId)
- **Exception Handling**: Global exception handler with centralized error logging
- **Events**: Application event publisher for business events (Observer pattern)
- **Validation**: Jakarta Bean Validation with custom validators

### Key Design Patterns

- **Layered Architecture**: Clear separation between controllers, services, and repositories
- **Observer Pattern**: Event-driven notifications (UserRegisteredEvent, BlogPostCreatedEvent)
- **Factory Pattern**: UserFactory for user entity creation
- **Strategy Pattern**: CurrentUserProvider for authentication context
- **Repository Pattern**: Spring Data JPA repositories with custom queries
- **DTO Pattern**: Request/Response DTOs to decouple API from domain models

For detailed architecture documentation, see [ARCHITECTURE.md](ARCHITECTURE.md).

For structured logging usage, see [LOGGING_GUIDE.md](LOGGING_GUIDE.md).

## Technologies

### Core Framework
- **Java 21**: Modern Java with Records, Pattern Matching, Virtual Threads
- **Spring Boot 3.5.3**: Application framework with auto-configuration
- **Spring Security 6**: Authentication and authorization with JWT
- **Spring Data JPA**: Data access abstraction with Hibernate ORM

### Security
- **JWT (JSON Web Tokens)**: Stateless authentication (io.jsonwebtoken:jjwt 0.12.6)
- **BCrypt**: Password hashing with adaptive complexity
- **Spring Security**: Method-level security with @PreAuthorize

### Database
- **PostgreSQL 16**: Primary relational database
- **Hibernate ORM**: Object-relational mapping with query optimization
- **H2 Database**: In-memory database for integration tests

### Logging & Monitoring
- **SLF4J + Logback**: Logging facade and implementation
- **Logstash Logback Encoder**: Structured JSON logging
- **Spring Actuator**: Health checks and metrics endpoints

### API Documentation
- **SpringDoc OpenAPI 3**: OpenAPI (Swagger) specification generation
- **Swagger UI**: Interactive API documentation

### Development Tools
- **Lombok**: Reduces boilerplate code with annotations
- **Jakarta Validation**: Bean validation with custom validators
- **JaCoCo**: Code coverage reporting with strategic focus on critical components

### Testing
- **JUnit 5**: Unit testing framework
- **Mockito**: Mocking framework for unit tests
- **Spring Security Test**: Security testing utilities
- **H2 Database**: In-memory database for integration tests

### Deployment
- **Docker**: Containerization
- **Docker Compose**: Multi-container orchestration
- **GraalVM Native Image**: AOT compilation for faster startup (optional)

## Getting Started

### Prerequisites

#### Option 1: Docker (Recommended)
- Docker
- Docker Compose

#### Option 2: Local Development
- JDK 21 or higher
- Maven
- PostgreSQL 16

### Running with Docker (Recommended)

The easiest way to run the application is using Docker Compose, which sets up everything automatically:

1. Clone the repository
2. Navigate to the project directory
3. Start all services:

```bash
docker-compose up -d
```

**Note:** The `-d` flag runs containers in detached mode (background). All services have `restart: unless-stopped` configured, so they'll automatically restart if they crash or after system reboot.

This command will:
- Build the Spring Boot application Docker image
- Start PostgreSQL 16 database
- Start pgAdmin for database management
- Start the auth service

**Access the services:**
- Auth Service: http://localhost:8080
- pgAdmin: http://localhost:5050 (login: admin@admin.com / admin)
- Swagger UI: http://localhost:8080/swagger-ui.html

**Useful Docker commands:**

```bash
# View logs
docker-compose logs -f auth-service

# Stop all services
docker-compose down

# Stop and remove volumes (clears database)
docker-compose down -v

# Rebuild and restart
docker-compose up --build -d

# View running containers
docker-compose ps
```

**pgAdmin Setup:**
1. Access pgAdmin at http://localhost:5050
2. Login with: admin@admin.com / admin
3. Add a new server:
   - Name: Auth Database
   - Host: postgres
   - Port: 5432
   - Database: auth
   - Username: postgres
   - Password: password

### Running with GraalVM Native Image (Advanced)

GraalVM Native Image provides significantly faster startup times and lower memory usage compared to the JVM version.

**Benefits:**
- Startup time: < 100ms (vs ~3-5 seconds with JVM)
- Memory usage: ~50-70% less than JVM
- Instant peak performance

**Build and run with GraalVM:**

```bash
docker-compose -f docker-compose.native.yml up --build -d
```

**Note:** Native image compilation takes 5-10 minutes on the first build, but the resulting image is much faster to start.

**Access the services (same as JVM version):**
- Auth Service: http://localhost:8080
- pgAdmin: http://localhost:5050
- Swagger UI: http://localhost:8080/swagger-ui.html

**Build native executable locally (requires GraalVM):**

```bash
# Install GraalVM 21 and set JAVA_HOME
# Then build native image
./mvnw -Pnative native:compile -DskipTests

# Run the native executable
./target/auth-service
```

### Running Locally (Without Docker)

#### Database Setup

1. Install PostgreSQL 16
2. Create a PostgreSQL database named `auth`:

```sql
CREATE DATABASE auth;
```

3. The application will automatically create tables using JPA

#### Running the Application

1. Clone the repository
2. Navigate to the project directory
3. Run the application using Maven:

```bash
mvn spring-boot:run
```

The application will start on port 8080 and connect to PostgreSQL on localhost:5432.

## API Documentation

### Swagger/OpenAPI

The API is documented using OpenAPI (Swagger). You can access the Swagger UI to explore and test the API endpoints:

- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- OpenAPI JSON: [http://localhost:8080/v1/api-docs](http://localhost:8080/v1/api-docs)

## API Endpoints

### Authentication Endpoints

#### Register a new user
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "password": "SecurePassword123"
}
```

**Response:**
```json
{
  "message": "User registered successfully"
}
```

#### Authenticate (Login)
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "john.doe@example.com",
  "password": "SecurePassword123"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

#### Refresh Token
```http
POST /api/v1/auth/refresh-token
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response:** Same as login response with new tokens.

### Blog Post Endpoints

All blog post endpoints require authentication via JWT Bearer token:
```http
Authorization: Bearer <access_token>
```

#### Get all blog posts (paginated)
```http
GET /api/v1/blog-posts?page=0&size=10&sort=createdAt,desc
```

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "title": "My First Post",
      "content": "This is the content...",
      "createdBy": {
        "id": 1,
        "firstName": "John",
        "lastName": "Doe",
        "email": "john.doe@example.com"
      },
      "createdAt": "2026-03-01T10:30:00",
      "updatedAt": "2026-03-01T10:30:00"
    }
  ],
  "pageable": {...},
  "totalElements": 25,
  "totalPages": 3
}
```

#### Get blog posts by user
```http
GET /api/v1/blog-posts/user/{userId}
```

#### Get a specific blog post
```http
GET /api/v1/blog-posts/{id}
```

#### Create a new blog post
```http
POST /api/v1/blog-posts
Content-Type: application/json

{
  "title": "My New Post",
  "content": "This is the content of my new post..."
}
```

**Response:** Returns the created blog post with `201 Created` status and `Location` header.

#### Update a blog post
```http
PUT /api/v1/blog-posts/{id}
Content-Type: application/json

{
  "title": "Updated Title",
  "content": "Updated content..."
}
```

**Authorization:** Only the creator of the post or users with ADMIN role can update.

#### Delete a blog post
```http
DELETE /api/v1/blog-posts/{id}
```

**Authorization:** Only the creator of the post or users with ADMIN role can delete.

**Response:** `204 No Content` on success.

### Test Endpoints

- `GET /api/v1/test/public` - Public endpoint (no authentication required)
- `GET /api/v1/test/protected` - Protected endpoint (requires authentication)
- `GET /api/v1/test/admin` - Admin endpoint (requires ADMIN role)

## Configuration

### Environment Variables

The application supports configuration via environment variables for flexible deployment:

**Database Configuration:**
- `DB_HOST` - Database host (default: localhost)
- `DB_PORT` - Database port (default: 5432)
- `DB_NAME` - Database name (default: auth)
- `DB_USER` - Database username (default: postgres)
- `DB_PASSWORD` - Database password (default: password)

**JWT Configuration:**
- `JWT_SECRET_KEY` - Secret key for JWT signing (default: provided in application.properties)
- `JWT_EXPIRATION` - Access token expiration in milliseconds (default: 900000 / 15 minutes)
- `JWT_REFRESH_EXPIRATION` - Refresh token expiration in milliseconds (default: 86400000 / 24 hours)

**Logging Configuration:**
- `SPRING_PROFILES_ACTIVE` - Set to `json-logs` for structured JSON logging (default: human-readable logs)

### Structured Logging

The application includes structured JSON logging for production environments:

**Enable JSON logging:**
```bash
# Set Spring profile
export SPRING_PROFILES_ACTIVE=json-logs
```

**Log Output Example:**
```json
{
  "timestamp": "2026-03-01T10:30:00.123Z",
  "level": "INFO",
  "logger": "com.harrish.auth.service.AuthenticationService",
  "message": "User authentication completed successfully",
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "userId": "123",
  "email": "user@example.com",
  "step": "complete",
  "duration_ms": 245
}
```

**Key Features:**
- **Request Correlation**: Every request has a unique `requestId` for tracing across services
- **User Context**: `userId` is automatically included in logs after authentication
- **Structured Fields**: Use `kv()` helper for consistent field naming
- **AI-Friendly**: JSON format is easy to parse and analyze with tools like jq, Elasticsearch, etc.

For more details on using structured logging, see [LOGGING_GUIDE.md](LOGGING_GUIDE.md).

### Docker Compose Configuration

**Standard JVM build** (`docker-compose.yml`):
- **PostgreSQL 16**: Alpine-based for smaller image size
- **pgAdmin**: Web-based database management tool
- **Auth Service**: Spring Boot application with health checks (JVM)
- **Named Volumes**: Persistent data storage for database and pgAdmin
- **Health Checks**: Ensures database is ready before starting the application

**GraalVM Native build** (`docker-compose.native.yml`):
- Same services as above
- **Auth Service**: Compiled as GraalVM native executable
- Faster startup and lower memory usage
- Separate volumes to avoid conflicts with JVM version

## Development

### Project Structure

```
auth/
├── src/
│   ├── main/
│   │   ├── java/com/harrish/auth/
│   │   │   ├── config/              # Spring configuration classes
│   │   │   ├── controller/          # REST API controllers
│   │   │   ├── dto/                 # Data Transfer Objects
│   │   │   ├── event/               # Application events
│   │   │   ├── exception/           # Custom exceptions and handlers
│   │   │   ├── filter/              # Request filters (MDC context)
│   │   │   ├── model/               # JPA entities
│   │   │   ├── repository/          # Spring Data JPA repositories
│   │   │   ├── security/            # Security components (JWT, UserPrincipal)
│   │   │   ├── service/             # Business logic layer
│   │   │   ├── util/                # Utility classes
│   │   │   └── AuthApplication.java # Main application class
│   │   └── resources/
│   │       ├── application.properties  # Application configuration
│   │       ├── logback-spring.xml      # Logging configuration
│   │       └── messages.properties     # i18n messages
│   └── test/                        # Unit and integration tests
├── Dockerfile                       # Multi-stage Docker build (JVM)
├── Dockerfile.native                # GraalVM native image build
├── docker-compose.yml               # Docker Compose for JVM version
├── docker-compose.native.yml        # Docker Compose for GraalVM native
├── .dockerignore                    # Docker build context exclusions
├── pom.xml                          # Maven dependencies with GraalVM profile
├── README.md                        # This file
├── ARCHITECTURE.md                  # Detailed architecture documentation
└── LOGGING_GUIDE.md                 # Structured logging guide
```

### Running Tests

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=ValidationUtilsTest

# Run tests with coverage report
./mvnw clean test jacoco:report

# View coverage report (after running tests)
open target/site/jacoco/index.html
```

**Test Coverage:** Strategic focus on critical components (see TEST_COVERAGE_REPORT.md for details)
- Security package: 80% coverage (authentication, JWT validation)
- Utility package: 59% coverage (validation helpers)
- Overall: 87 unit tests passing
- Coverage report: `target/site/jacoco/index.html`

For more details, see [TEST_COVERAGE_REPORT.md](TEST_COVERAGE_REPORT.md).

### Building the Docker Image

**Build JVM version:**
```bash
docker build -t auth-service:latest .
```

**Build GraalVM native version:**
```bash
docker build -f Dockerfile.native -t auth-service:native .
```

**Note:** GraalVM native build takes significantly longer (5-10 minutes) but produces a faster executable.

## Security Features

### Authentication & Authorization
- **JWT-based authentication**: Stateless token-based authentication
- **Password hashing**: BCrypt with adaptive complexity (cost factor 10)
- **Token expiration**: Short-lived access tokens (15 minutes) + long-lived refresh tokens (24 hours)
- **Method-level security**: Fine-grained authorization with `@PreAuthorize`
- **Role-based access control**: USER and ADMIN roles

### Security Best Practices
- **Input validation**: Jakarta Bean Validation with custom validators
- **SQL injection prevention**: Parameterized queries via JPA/Hibernate
- **XSS protection**: Spring Security's default headers
- **CORS configuration**: Configured in SecurityConfiguration
- **Error message sanitization**: Generic error messages to prevent information disclosure

### Security Audit Reports
- See [SECURITY_AUDIT_REPORT.md](SECURITY_AUDIT_REPORT.md) for detailed security audit results
- See [SECURITY.md](SECURITY.md) for security policy and vulnerability reporting

## Performance Optimizations

### Database Optimizations
- **N+1 query prevention**: Optimized JPA queries with JOIN FETCH (99.5% query reduction)
- **Database indexing**: Strategic indexes on frequently queried columns
  - `idx_user_email` on users.email (unique)
  - `idx_blog_post_created_by` on blog_posts.created_by_id
  - `idx_blog_post_created_at` on blog_posts.created_at
- **Connection pooling**: HikariCP for efficient connection management
- **Query result caching**: JWT signing key cached to avoid repeated key derivation

### Concurrency Optimizations
- **Bounded thread pool**: Prevents OutOfMemoryError from unbounded thread creation
- **Async operations**: `@Async` for non-blocking event publishing
- **SecurityContext propagation**: Automatic security context copy to async threads

### Performance Metrics
- **Response time improvement**: 500ms → 10ms (98% faster) after N+1 fix
- **Memory usage**: 1GB → 10MB (99% reduction) under load with bounded thread pool
- **Startup time**: < 100ms with GraalVM native image (vs 3-5 seconds JVM)

For detailed performance reports:
- See [SPRING_BOOT_JPA_API_REPORT.md](SPRING_BOOT_JPA_API_REPORT.md) for N+1 query fixes
- See [PERFORMANCE_CONCURRENCY_REPORT.md](PERFORMANCE_CONCURRENCY_REPORT.md) for concurrency improvements

## Documentation

### Core Documentation (Start Here)
- **[README.md](README.md)**: Getting started guide and API reference (this file)
- **[ARCHITECTURE.md](ARCHITECTURE.md)**: System architecture, design patterns, and scalability assessment
- **[LOGGING_GUIDE.md](LOGGING_GUIDE.md)**: Structured logging best practices with MDC context
- **[SECURITY.md](SECURITY.md)**: Security policy and deployment checklist

### Phase Reports & Audits
- **[TEST_COVERAGE_REPORT.md](TEST_COVERAGE_REPORT.md)**: Test coverage analysis (Phase 5 - 44% strategic coverage)
- **[SECURITY_AUDIT_REPORT.md](SECURITY_AUDIT_REPORT.md)**: Security audit with fix status updates (Phase 1.1)
- **[PERFORMANCE_CONCURRENCY_REPORT.md](PERFORMANCE_CONCURRENCY_REPORT.md)**: Performance and async improvements (Phase 4)
- **[SPRING_BOOT_JPA_API_REPORT.md](SPRING_BOOT_JPA_API_REPORT.md)**: N+1 query fixes and indexing (Phase 3)
- **[DEPENDENCY_AUDIT_REPORT.md](DEPENDENCY_AUDIT_REPORT.md)**: Maven dependency audit (Phase 1.2)

### Code Quality Reviews (Reference)
- **[CODE_REVIEW_REPORT.md](CODE_REVIEW_REPORT.md)**: Java code review findings (Phase 2.1)
- **[SOLID_PRINCIPLES_REPORT.md](SOLID_PRINCIPLES_REPORT.md)**: SOLID principles analysis (Phase 2.2)
- **[CLEAN_CODE_REPORT.md](CLEAN_CODE_REPORT.md)**: Clean code review (Phase 2.3)
- **[DESIGN_PATTERNS_REPORT.md](DESIGN_PATTERNS_REPORT.md)**: Design patterns implemented (Phase 2.4)
- **[SPRING_BOOT_JPA_API_REPORT.md](SPRING_BOOT_JPA_API_REPORT.md)**: Spring Boot/JPA/API patterns
- **[PERFORMANCE_CONCURRENCY_REPORT.md](PERFORMANCE_CONCURRENCY_REPORT.md)**: Performance and concurrency

## Contributing

This project follows clean code principles, SOLID design principles, and Spring Boot best practices. Before contributing, please review the architecture and design pattern documentation.

## License

[Add your license information here]