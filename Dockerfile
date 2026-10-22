# Multi-stage Docker build for AI-Based Disaster Prediction & Emergency Response System
FROM maven:3.9.8-eclipse-temurin-17 AS builder
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime Stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Create non-root user
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
