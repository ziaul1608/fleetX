# ==============================================================================
# FleetX Production Container Multi-Stage Dockerfile
# Single Public Origin Architecture (React SPA + Spring Boot REST API + WebSockets)
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build the React 18 + Vite frontend
# ------------------------------------------------------------------------------
FROM node:20-alpine AS frontend-builder
WORKDIR /app/frontend

# Copy dependencies definitions
COPY frontend/package*.json ./
RUN npm ci

# Copy frontend source and build static bundle
COPY frontend/ ./
RUN npm run build

# ------------------------------------------------------------------------------
# Stage 2: Build Spring Boot 3.5.x Fat JAR with embedded static assets
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS backend-builder
WORKDIR /app

# Prepare Maven wrapper and project model
COPY backend/pom.xml backend/
COPY backend/.mvn backend/.mvn
COPY backend/mvnw backend/
RUN chmod +x backend/mvnw

# Copy backend source code
COPY backend/src backend/src

# Copy compiled frontend assets from Stage 1 into backend static resource directory
COPY --from=frontend-builder /app/frontend/dist backend/src/main/resources/static

# Package application into a single executable fat JAR
RUN cd backend && ./mvnw clean package -DskipTests

# ------------------------------------------------------------------------------
# Stage 3: Minimal, unprivileged Java 21 JRE runtime image
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

# Install curl for health checks and network diagnostics
RUN apk add --no-cache curl

# Create unprivileged system user and group
RUN addgroup -S fleetx && adduser -S fleetx -G fleetx

WORKDIR /app

# Copy the built fat JAR from Stage 2
COPY --from=backend-builder --chown=fleetx:fleetx /app/backend/target/fleetx-backend-0.0.1-SNAPSHOT.jar /app/fleetx.jar

# Run as non-root user
USER fleetx

# Default runtime configuration
ENV PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:${PORT:-8080}/api/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/fleetx.jar"]
