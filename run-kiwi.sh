#!/bin/bash
set -e

# Cargar variables de entorno
source load-env.sh

# Configurar variables requeridas
export DB_URL="${DB_URL:-jdbc:postgresql://localhost:5432/kiwi}"
export DB_USER="${DB_USER:-kiwi_user}"
export DB_PASSWORD="${DB_PASSWORD:-password}"
export JWT_SECRET="${JWT_SECRET:-test-secret-32-chars-long-123456789}"
export JWT_ISS="${JWT_ISS:-dev.rafex.kiwi}"
export JWT_AUD="${JWT_AUD:-kiwi-backend}"
export JWT_TTL_SECONDS="${JWT_TTL_SECONDS:-3600}"
export PORT="${PORT:-8080}"
export LOG_LEVEL="${LOG_LEVEL:-INFO}"

echo "=== Iniciando Kiwi Backend ==="
echo "DB_URL: $DB_URL"
echo "DB_USER: $DB_USER"
echo "JWT_ISS: $JWT_ISS"
echo "PORT: $PORT"
echo "LOG_LEVEL: $LOG_LEVEL"
echo "=============================="

cd backend/java/kiwi-parent
java -jar kiwi-transport-jetty/target/kiwi-transport-jetty-0.1.0-SNAPSHOT-jar-with-dependencies.jar --log="$LOG_LEVEL"