# Auth Kotlin Service

A Spring Boot 4.0.3 application implemented in Kotlin 2.0.21, demonstrating Spring Security with JWT (JSON Web Token) authentication and basic blog post management.

## Overview

This project provides a robust starting point for building secured REST APIs with Kotlin and Spring Boot. It includes:
- **Authentication & Authorization**: JWT-based security with registration, login, and token refresh capabilities.
- **Auditing**: Automatic tracking of entity creation and modification times.
- **API Documentation**: Integrated Swagger UI/OpenAPI for easy API exploration.
- **Error Handling**: Centralized global exception handling with structured error responses.
- **Database Persistence**: PostgreSQL integration with Spring Data JPA.

## Tech Stack

- **Language**: Kotlin 2.0.21
- **Runtime**: JVM (JDK 21)
- **Framework**: Spring Boot 4.0.3
- **Security**: Spring Security, JJWT (io.jsonwebtoken)
- **Database**: PostgreSQL
- **Documentation**: SpringDoc OpenAPI (Swagger UI)
- **Build Tool**: Maven

## Requirements

- **JDK 21** or higher
- **Maven 3.9+**
- **PostgreSQL** instance

## Setup & Run

### 1. Database Setup
Create a PostgreSQL database named `auth`:
```sql
CREATE DATABASE auth;
```

### 2. Configuration
Update the database credentials and JWT settings in `src/main/resources/application.properties` if necessary:
- `spring.datasource.url`: Database connection string
- `spring.datasource.username`: Your database username
- `spring.datasource.password`: Your database password
- `jwt.secret`: A secure key for signing tokens (minimum 256-bit recommended)

### 3. Build the Application
```bash
mvn clean install
```

### 4. Run the Application
```bash
mvn spring-boot:run
```
The server will start on `http://localhost:8080`.

## Scripts & Entry Points

- **Main Entry Point**: `com.harrish.auth.AuthApplicationKt`
- **Maven Commands**:
    - `mvn clean`: Clean the build directory.
    - `mvn compile`: Compile source code.
    - `mvn package`: Bundle the application into a JAR file.
    - `mvn spring-boot:run`: Run the application locally.

## Environment Variables / Properties

| Property | Description | Default Value |
|----------|-------------|---------------|
| `server.port` | Application port | `8080` |
| `spring.datasource.url` | JDBC URL for PostgreSQL | `jdbc:postgresql://localhost:5432/auth` |
| `jwt.secret` | Secret key for JWT generation | `MySuperSecretKey...` |
| `jwt.expiration` | Access token expiration (ms) | `3600000` (1 hour) |
| `jwt.refresh-expiration` | Refresh token expiration (ms) | `86400000` (24 hours) |

## API Documentation

Once the application is running, you can access the interactive API documentation at:
- **Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI Spec**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

## Project Structure

```text
src/main/kotlin/com/harrish/auth/
├── config/             # Configuration classes (Security, JPA, OpenAPI)
├── controller/         # REST API Controllers
├── dto/                # Data Transfer Objects for API requests/responses
├── exception/          # Custom exceptions and Global Exception Handler
├── model/              # JPA Entities
├── repository/         # Spring Data JPA Repositories
├── security/           # JWT and Security implementation details
├── service/            # Business Logic layer
└── util/               # Utility classes
```

## Tests

- **TODO**: Add unit and integration tests. The `src/test` directory is currently initialized but lacks test implementations.
- To run tests (when added):
  ```bash
  mvn test
  ```

## License

- **TODO**: Add license information (e.g., MIT, Apache 2.0).
