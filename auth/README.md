# Auth Service

A Spring Boot application providing authentication and blog post management with JWT security, structured logging, and comprehensive test coverage.

## Features

- User registration and authentication with JWT
- Token refresh mechanism
- Blog post CRUD with authorization
- RESTful API with RFC 7807 Problem Details
- Internationalization support
- Structured JSON logging with request correlation
- Event-driven architecture
- Optimized JPA queries
- Database indexing

## Architecture

### High-Level Overview

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
- **Exception Handling**: Global exception handler
- **Events**: Application event publisher (Observer pattern)
- **Validation**: Jakarta Bean Validation

### Key Design Patterns

- **Layered Architecture**: Clear separation between controllers, services, and repositories
- **Observer Pattern**: Event-driven notifications
- **Factory Pattern**: UserFactory for entity creation
- **Strategy Pattern**: CurrentUserProvider for authentication context
- **Repository Pattern**: Spring Data JPA repositories
- **DTO Pattern**: Request/Response DTOs

See [ARCHITECTURE.md](ARCHITECTURE.md) for details.

## Technologies

### Core Framework
- **Java 21**: Records, Pattern Matching, Virtual Threads
- **Spring Boot 3.5.3**: Application framework
- **Spring Security 6**: JWT authentication
- **Spring Data JPA**: Data access with Hibernate

### Security
- **JWT**: Stateless authentication (io.jsonwebtoken:jjwt 0.12.6)
- **BCrypt**: Password hashing
- **Method-level security**: @PreAuthorize

### Database
- **PostgreSQL 16**: Primary database
- **H2**: In-memory database for tests

### Logging & Monitoring
- **SLF4J + Logback**: Logging
- **Logstash Encoder**: JSON logging
- **Spring Actuator**: Health checks

### API Documentation
- **SpringDoc OpenAPI 3**: OpenAPI specification
- **Swagger UI**: Interactive documentation

### Testing
- **JUnit 5**: Testing framework
- **Mockito**: Mocking
- **Spring Security Test**: Security testing
- **JaCoCo**: Code coverage

### Deployment
- **Docker**: Containerization
- **Docker Compose**: Multi-container orchestration
- **GraalVM Native Image**: AOT compilation (optional)

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

```bash
docker-compose up -d
```

This will:
- Build the Spring Boot application
- Start PostgreSQL 16
- Start pgAdmin
- Start the auth service

**Access:**
- Auth Service: http://localhost:8080
- pgAdmin: http://localhost:5050 (login: admin@admin.com / admin)
- Swagger UI: http://localhost:8080/swagger-ui.html

**Commands:**

```bash
# View logs
docker-compose logs -f auth-service

# Stop services
docker-compose down

# Rebuild
docker-compose up --build -d
```

**pgAdmin Setup:**
1. Access http://localhost:5050
2. Login: admin@admin.com / admin
3. Add server:
   - Host: postgres
   - Port: 5432
   - Database: auth
   - Username: postgres
   - Password: password

### Running with GraalVM Native Image

For faster startup and lower memory usage:

```bash
docker-compose -f docker-compose.native.yml up --build -d
```

### Running Locally

#### Database Setup

```sql
CREATE DATABASE auth;
```

#### Run Application

```bash
mvn spring-boot:run
```

## API Documentation

### Swagger/OpenAPI

- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- OpenAPI JSON: [http://localhost:8080/v1/api-docs](http://localhost:8080/v1/api-docs)

## API Endpoints

### Authentication

#### Register
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

#### Login
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

### Blog Posts

All endpoints require authentication:
```http
Authorization: Bearer <access_token>
```

#### Get all posts (paginated)
```http
GET /api/v1/blog-posts?page=0&size=10&sort=createdAt,desc
```

#### Get posts by user
```http
GET /api/v1/blog-posts/user/{userId}
```

#### Get specific post
```http
GET /api/v1/blog-posts/{id}
```

#### Create post
```http
POST /api/v1/blog-posts
Content-Type: application/json

{
  "title": "My Post",
  "content": "Content..."
}
```

#### Update post
```http
PUT /api/v1/blog-posts/{id}
Content-Type: application/json

{
  "title": "Updated Title",
  "content": "Updated content..."
}
```

**Authorization:** Only creator or ADMIN can update.

#### Delete post
```http
DELETE /api/v1/blog-posts/{id}
```

**Authorization:** Only creator or ADMIN can delete.

### Test Endpoints

- `GET /api/v1/test/public` - Public endpoint
- `GET /api/v1/test/protected` - Requires authentication
- `GET /api/v1/test/admin` - Requires ADMIN role

## Configuration

### Environment Variables

**Database:**
- `DB_HOST` - Database host (default: localhost)
- `DB_PORT` - Database port (default: 5432)
- `DB_NAME` - Database name (default: auth)
- `DB_USER` - Database username (default: postgres)
- `DB_PASSWORD` - Database password (default: password)

**JWT:**
- `JWT_SECRET_KEY` - Secret key for JWT signing
- `JWT_EXPIRATION` - Access token expiration in milliseconds (default: 900000)
- `JWT_REFRESH_EXPIRATION` - Refresh token expiration in milliseconds (default: 86400000)

**Logging:**
- `SPRING_PROFILES_ACTIVE` - Set to `json-logs` for JSON logging

### Structured Logging

Enable JSON logging:
```bash
export SPRING_PROFILES_ACTIVE=json-logs
```

**Output Example:**
```json
{
  "timestamp": "2026-03-01T10:30:00.123Z",
  "level": "INFO",
  "logger": "com.harrish.auth.service.AuthenticationService",
  "message": "User authentication completed successfully",
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "userId": "123",
  "email": "user@example.com"
}
```

See [LOGGING_GUIDE.md](LOGGING_GUIDE.md) for details.

## Development

### Project Structure

```
auth/
├── src/
│   ├── main/
│   │   ├── java/com/harrish/auth/
│   │   │   ├── config/              # Configuration
│   │   │   ├── controller/          # REST controllers
│   │   │   ├── dto/                 # Data Transfer Objects
│   │   │   ├── event/               # Events
│   │   │   ├── exception/           # Exception handlers
│   │   │   ├── filter/              # Filters
│   │   │   ├── model/               # JPA entities
│   │   │   ├── repository/          # Repositories
│   │   │   ├── security/            # Security components
│   │   │   ├── service/             # Business logic
│   │   │   └── util/                # Utilities
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── logback-spring.xml
│   │       └── messages.properties
│   └── test/                        # Tests
├── Dockerfile
├── Dockerfile.native
├── docker-compose.yml
├── docker-compose.native.yml
├── pom.xml
├── README.md
├── ARCHITECTURE.md
├── LOGGING_GUIDE.md
└── SECURITY.md
```

### Running Tests

```bash
# Run all tests
./mvnw test

# Run specific test
./mvnw test -Dtest=ValidationUtilsTest

# Run with coverage
./mvnw clean test jacoco:report

# View coverage
open target/site/jacoco/index.html
```

### Building Docker Image

**JVM version:**
```bash
docker build -t auth-service:latest .
```

**GraalVM native:**
```bash
docker build -f Dockerfile.native -t auth-service:native .
```

## Security Features

### Authentication & Authorization
- JWT-based authentication
- BCrypt password hashing
- Short-lived access tokens
- Long-lived refresh tokens
- Method-level security with @PreAuthorize
- Role-based access control

### Best Practices
- Input validation
- SQL injection prevention via JPA
- XSS protection
- CORS configuration
- Error message sanitization

See [SECURITY.md](SECURITY.md) for security policy.

## Performance Optimizations

### Database
- N+1 query prevention with JOIN FETCH
- Strategic indexes:
  - `idx_user_email` on users.email
  - `idx_blog_post_created_by` on blog_posts.created_by_id
  - `idx_blog_post_created_at` on blog_posts.created_at
- HikariCP connection pooling
- JWT signing key caching

### Concurrency
- Bounded thread pool
- @Async for event publishing
- SecurityContext propagation to async threads

## Documentation

- **[README.md](README.md)**: Getting started guide (this file)
- **[ARCHITECTURE.md](ARCHITECTURE.md)**: System architecture and design patterns
- **[LOGGING_GUIDE.md](LOGGING_GUIDE.md)**: Structured logging guide
- **[SECURITY.md](SECURITY.md)**: Security policy

## Contributing

This project follows clean code principles, SOLID design, and Spring Boot best practices.

## License

[Add license information]
