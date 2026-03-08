#!/bin/bash
# Build script for auth-kotlin application using Docker for compilation

echo "Building application with Maven in Docker (Java 21) using host network..."

# Build using Maven Docker image with Java 21 and host network mode
docker run --rm \
  --network=host \
  -v "$PWD":/app \
  -v "$HOME/.m2":/root/.m2 \
  -w /app \
  maven:3.9-eclipse-temurin-21 \
  mvn clean package -DskipTests

if [ $? -eq 0 ]; then
    echo "Build successful! Now building Docker image..."
    docker build -t auth-kotlin .
    
    if [ $? -eq 0 ]; then
        echo "Docker image built successfully!"
        echo "Run 'docker-compose up' to start the application"
    else
        echo "Docker build failed!"
        exit 1
    fi
else
    echo "Maven build failed!"
    exit 1
fi
