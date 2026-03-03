# CollegeUpdates Spring Boot Backend
# Uses pre-built JAR from target/ (Maven build runs locally before docker build)
FROM eclipse-temurin:21-jre-alpine

# Add non-root user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

WORKDIR /app

# Copy the pre-built JAR from target/
COPY target/demo-college-update-0.0.1-SNAPSHOT.jar app.jar

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
# Profile is set via SPRING_PROFILES_ACTIVE env var in Cloud Run
ENTRYPOINT ["java", "-jar", "app.jar"]
CMD []