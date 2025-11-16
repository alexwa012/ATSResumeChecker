# =========================
# 1. Build Stage
# =========================
FROM maven:3.9-eclipse-temurin-17 AS build

# Set working directory
WORKDIR /app

# Copy Maven descriptor first (for better layer caching)
COPY pom.xml .

# Download dependencies (this will be cached if pom.xml doesn't change)
RUN mvn -q dependency:go-offline

# Now copy the source
COPY src ./src

# Build the application (skip tests for faster image builds)
RUN mvn -q clean package -DskipTests


# =========================
# 2. Runtime Stage
# =========================
FROM eclipse-temurin:17-jre-alpine

# Set a non-root user (optional but recommended)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/target/atsResumeChecker-0.0.1-SNAPSHOT.jar app.jar

# Expose app port (must match server.port in application.properties)
EXPOSE 8080

# Environment variables (Perplexity key will be passed at runtime)
ENV JAVA_OPTS=""
# Don't hardcode PERPLEXITY_API_KEY here, pass it via `docker run -e ...`

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
