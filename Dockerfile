# Stage 1: Build Stage
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml for dependency caching
COPY legal-assistance-backend/.mvn/ legal-assistance-backend/.mvn/
COPY legal-assistance-backend/mvnw legal-assistance-backend/pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B || true

# Copy source code and build production executable JAR
COPY legal-assistance-backend/src ./src
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Production Execution Stage
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# Create non-root system group and user for security hardening
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy compiled executable JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Adjust permissions
RUN chown -R appuser:appgroup /app

# Run as non-root user
USER appuser

# Expose Spring Boot default port
EXPOSE 8080

# Production Java runtime optimization flags
ENV PORT=8080 \
    JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Launch Spring Boot application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
