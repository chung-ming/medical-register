# Stage 1: Build the application using Maven
# Use an official Maven image with JDK 21 (as specified in pom.xml)
FROM maven:3.9.6-eclipse-temurin-21 AS builder

# Set the working directory in the container
WORKDIR /app

# Copy the pom.xml first to leverage Docker cache for dependencies
COPY pom.xml .
# Download dependencies (offline mode ensures it only uses what's declared)
RUN mvn dependency:go-offline -B

# Copy the rest of your application's source code
COPY src ./src

# Package the application, skipping tests for faster Docker image builds
RUN mvn package -DskipTests

# Stage 2: Create the runtime image
# Use a slim JRE 21 image for a smaller final image size
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the JAR file from the builder stage
# The JAR name pattern 'medical-register-*.jar' assumes your artifactId is 'medical-register'.
# Adjust this if your pom.xml produces a differently named JAR in the 'target' directory.
COPY --from=builder /app/target/medical-register-*.jar app.jar

# Expose the port your application runs on (as defined in application.properties)
EXPOSE 8080

# Set the default Spring profile to 'prod' when running in Docker.
# This will make Spring Boot use 'application-prod.properties'.
ENV SPRING_PROFILES_ACTIVE=prod

# Command to run the application when the container starts
ENTRYPOINT ["java", "-jar", "app.jar"]