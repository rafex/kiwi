FROM docker.io/eclipse-temurin:25.0.2_10-jre

WORKDIR /app

# Copy the fat JAR produced by the build
# Ensure you run 'make build' before building this image so target/ exists
COPY target/kiwi-backend-1.0-SNAPSHOT-jar-with-dependencies.jar /app/app.jar

# Environment variables for configuration (set at runtime)
ENV DB_USER=""
ENV DB_PASSWORD=""
ENV DB_URL=""

# Expose service port
EXPOSE 8080

# Run as non-root user (image provides a default user with id 1000)
USER 1000

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

