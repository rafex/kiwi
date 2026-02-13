FROM docker.io/eclipse-temurin:25.0.2_10-jre

# Non-interactive
ENV DEBIAN_FRONTEND=noninteractive

# Create a user to run the app
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates \
	&& apt-get clean && rm -rf /var/lib/apt/lists/* \
	&& useradd -r -s /usr/sbin/nologin -m -U kiwi || true

WORKDIR /app

# Copy the fat JAR produced by the build
# Ensure you run 'make build' before building this image so target/ exists
COPY target/kiwi-backend-1.0-SNAPSHOT-jar-with-dependencies.jar /app/app.jar

# Copy launcher script
COPY start-kiwi.sh /usr/local/bin/start-kiwi.sh

RUN chmod +x /usr/local/bin/start-kiwi.sh \
	&& mkdir -p /etc/kiwi-env \
	&& chown -R kiwi:kiwi /app /etc/kiwi-env /usr/local/bin/start-kiwi.sh

# Environment variables are accepted at runtime; do not declare sensitive defaults here

# Expose service port
EXPOSE 8080

# Run as non-root user
USER kiwi

# Start the wrapper script (will exec the jar)
ENTRYPOINT ["/usr/local/bin/start-kiwi.sh"]

