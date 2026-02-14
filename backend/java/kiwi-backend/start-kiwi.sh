#!/bin/sh
# Use POSIX-compatible options; avoid 'pipefail' which is not supported by /bin/sh
set -eu

# start-kiwi: launcher for the fat JAR
# - honors env vars DB_USER, DB_PASSWORD, DB_URL
# - accepts optional JAVA_OPTS

JAVA_OPTS="${JAVA_OPTS:-}"
DB_USER="${DB_USER:-}"
DB_PASSWORD="${DB_PASSWORD:-}"
DB_URL="${DB_URL:-}"
LOG_LEVEL="${LOG_LEVEL:-INFO}"

if [ -z "$DB_URL" ]; then
  echo "[start-kiwi] WARNING: DB_URL is not set. The application may fail to connect to the database." >&2
fi

echo "[start-kiwi] Starting kiwi-backend (DB_URL=${DB_URL:+<provided>})"

exec java $JAVA_OPTS -jar /app/app.jar "$@"
