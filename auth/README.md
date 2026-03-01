# Auth Service

A Spring Boot application that provides authentication and blog post management functionality.

## Features

- User registration and authentication with JWT
- Token refresh mechanism
- Blog post CRUD operations with authorization
- RESTful API design
- Proper error handling
- Internationalization support

## Technologies

- Java 21
- Spring Boot 3.5.3
- Spring Security with JWT
- Spring Data JPA
- PostgreSQL 16
- Lombok
- Jakarta Validation
- Docker & Docker Compose
- GraalVM Native Image Support

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

### Test Endpoints

- `GET /api/v1/test/public` - Public endpoint (no authentication required)
- `GET /api/v1/test/protected` - Protected endpoint (requires authentication)
- `GET /api/v1/test/admin` - Admin endpoint (requires ADMIN role)

### Authentication

- `POST /api/v1/auth/register` - Register a new user
- `POST /api/v1/auth/login` - Authenticate a user and get JWT tokens
- `POST /api/v1/auth/refresh-token` - Refresh JWT token

### Blog Posts

- `GET /api/v1/blog-posts` - Get all blog posts (paginated)
- `GET /api/v1/blog-posts/user/{userId}` - Get blog posts by user
- `GET /api/v1/blog-posts/{id}` - Get a specific blog post
- `POST /api/v1/blog-posts` - Create a new blog post
- `PUT /api/v1/blog-posts/{id}` - Update a blog post (requires authorization)
- `DELETE /api/v1/blog-posts/{id}` - Delete a blog post (requires authorization)

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
│   │   ├── java/
│   │   │   └── com/harrish/auth/
│   │   └── resources/
│   │       └── application.properties
│   └── test/
├── Dockerfile                   # Multi-stage Docker build (JVM)
├── Dockerfile.native            # GraalVM native image build
├── docker-compose.yml           # Docker Compose for JVM version
├── docker-compose.native.yml    # Docker Compose for GraalVM native
├── .dockerignore               # Docker build context exclusions
└── pom.xml                     # Maven dependencies with GraalVM profile
```

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