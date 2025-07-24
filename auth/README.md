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

- Java 17
- Spring Boot 3.x
- Spring Security with JWT
- Spring Data JPA
- PostgreSQL
- Lombok
- Jakarta Validation

## Getting Started

### Prerequisites

- JDK 21 or higher
- Maven
- PostgreSQL

### Database Setup

1. Create a PostgreSQL database named `auth`
2. Update the database configuration in `application.properties` if needed

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/auth
spring.datasource.username=postgres
spring.datasource.password=password
```

### Running the Application

1. Clone the repository
2. Navigate to the project directory
3. Run the application using Maven:

```bash
mvn spring-boot:run
```

The application will start on port 8080.

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