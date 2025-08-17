# Build stage
FROM node:22-alpine AS frontend-build

WORKDIR /app/frontend

COPY frontend/ ./

RUN npm ci --only=production && \
    npm run build

# Application stage
FROM eclipse-temurin:21-jdk-alpine

LABEL maintainer="SqlApp2 Development Team"
LABEL description="SqlApp2 - Web-based SQL execution tool"

# Create app user with fixed UID/GID for volume permissions
RUN addgroup -g 1001 sqlapp2 && \
    adduser -u 1001 -G sqlapp2 -D -s /bin/false sqlapp2

# Install dependencies
RUN apk update && \
    apk add curl

WORKDIR /app

# Copy built application
COPY build/libs/sqlapp2.war app.war

# Create directories for H2 database and logs with proper permissions
RUN mkdir -p /app/data /app/logs && \
    chown -R 1001:1001 /app && \
    chmod 755 /app/data /app/logs

USER 1001:1001

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.war"]
