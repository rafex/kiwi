#!/usr/bin/env bash
set -euo pipefail

# =========================
# Configurable
# =========================
APP_JAR_DEFAULT="target/kiwi-backend-1.0-SNAPSHOT-jar-with-dependencies.jar"
OUTPUT_BIN_DEFAULT="kiwi-backend.bin"
GRAAL_CONFIG_DIR_DEFAULT="graal-config"
RES_DIR_DEFAULT="META-INF/native-image"

# Run agent? (0 = no, 1 = yes)
RUN_AGENT=0

# Maven command (wrapper preferred)
MVN_CMD="./mvnw"

# =========================
# Args parsing (minimal)
# =========================
APP_JAR="${APP_JAR_DEFAULT}"
OUTPUT_BIN="${OUTPUT_BIN_DEFAULT}"
GRAAL_CONFIG_DIR="${GRAAL_CONFIG_DIR_DEFAULT}"
RES_DIR="${RES_DIR_DEFAULT}"
MARCH_FLAGS=()

usage() {
  cat <<USAGE
Usage:
  ./native_build.sh [--run-agent] [--jar PATH] [--out NAME] [--graal-config DIR] [--res-dir DIR]

Examples:
  ./native_build.sh
  ./native_build.sh --run-agent
  ./native_build.sh --jar target/app.jar --out app-native
  ./native_build.sh --march x86-64-v2 --march x86-64-v3

Notes:
  You can pass multiple --march options; each will be forwarded to native-image as
  "--march=<value>" (e.g. --march x86-64-v2).

Notes:
  --run-agent starts the app with native-image-agent. You MUST hit your endpoints while it's running.
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --run-agent) RUN_AGENT=1; shift ;;
    --jar) APP_JAR="$2"; shift 2 ;;
    --out) OUTPUT_BIN="$2"; shift 2 ;;
    --graal-config) GRAAL_CONFIG_DIR="$2"; shift 2 ;;
    --res-dir) RES_DIR="$2"; shift 2 ;;
    --march) MARCH_FLAGS+=("--march=$2"); shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown arg: $1"; usage; exit 2 ;;
  esac
done

# =========================
# Helpers
# =========================
die() { echo "ERROR: $*" >&2; exit 1; }

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "Missing command: $1"
}

# =========================
# Preconditions
# =========================
need_cmd java
need_cmd uname
need_cmd mkdir
need_cmd cp
need_cmd rm
need_cmd grep

if [[ -x "$MVN_CMD" ]]; then
  :
else
  need_cmd mvn
  MVN_CMD="mvn"
fi

# Check GraalVM
JAVA_VERSION_OUT="$(java --version 2>/dev/null || true)"
if ! echo "$JAVA_VERSION_OUT" | grep -qi "GraalVM"; then
  echo "$JAVA_VERSION_OUT"
  die "Your current 'java' is NOT GraalVM. Export JAVA_HOME to GraalVM and put \$JAVA_HOME/bin first in PATH."
fi

# Check native-image
if ! command -v native-image >/dev/null 2>&1; then
  die "native-image not found. Install it (e.g. 'gu install native-image') or adjust PATH to GraalVM that has it."
fi

# =========================
# 1) Build jar (ensure exists)
# =========================
echo "[1/5] Building jar with Maven..."
"$MVN_CMD" -q -DskipTests package

if [[ ! -f "$APP_JAR" ]]; then
  echo
  echo "Built artifacts under target/:"
  ls -la target || true
  echo
  die "Jar not found at: $APP_JAR (use --jar PATH to point to the right one)"
fi

# =========================
# 2) Run agent (optional)
# =========================
if [[ "$RUN_AGENT" -eq 1 ]]; then
  echo "[2/5] Running app with native-image-agent..."
  rm -rf "$GRAAL_CONFIG_DIR"
  mkdir -p "$GRAAL_CONFIG_DIR"

  echo ">> App will start now. IMPORTANT: hit your endpoints (curl/Postman) so the agent captures reflection/resources."
  echo ">> Stop the app when you're done (Ctrl+C)."

  # Run and let user exercise routes
  java -agentlib:native-image-agent=config-output-dir="./$GRAAL_CONFIG_DIR" -jar "$APP_JAR"
fi

# =========================
# 3) Copy generated metadata/config into resources
# =========================
echo "[3/5] Sync Graal metadata/config into resources: $RES_DIR"
mkdir -p "$RES_DIR"

# Copy reachability metadata if present
if [[ -f "$GRAAL_CONFIG_DIR/reachability-metadata.json" ]]; then
  cp -f "$GRAAL_CONFIG_DIR/reachability-metadata.json" "$RES_DIR/"
  echo "  - Copied reachability-metadata.json"
fi

# Copy legacy configs if present (reflect/resource/proxy/jni/etc)
# They are often under META-INF/native-image/<group>/<artifact>/
if [[ -d "$GRAAL_CONFIG_DIR/META-INF/native-image" ]]; then
  # Copy entire native-image tree into resources, preserving structure.
  cp -a "$GRAAL_CONFIG_DIR/META-INF/native-image" "$RES_DIR/.."/
  echo "  - Copied META-INF/native-image/*"
fi

# Sanity check: show what ended up in resources
echo "  - Resulting files:"
find "$RES_DIR/.." -maxdepth 5 -type f -name "*.json" -print | sed 's/^/    /' || true

# =========================
# 4) Rebuild jar so metadata travels inside it
# =========================
echo "[4/5] Rebuilding jar to include META-INF/native-image metadata..."
"$MVN_CMD" -q -DskipTests package

# =========================
# 5) Build native image
# =========================
if [[ ${#MARCH_FLAGS[@]} -eq 0 ]]; then
  echo "[5/5] Building native image: $OUTPUT_BIN"
  # For many apps, using the jar directly is enough once metadata is on classpath.
  native-image --no-fallback -jar "$APP_JAR" "$OUTPUT_BIN"
else
  echo "[5/5] Building native images for marches: ${MARCH_FLAGS[*]}"
  # derive base name and extension from OUTPUT_BIN
  OUTPUT_BASE="$OUTPUT_BIN"
  OUTPUT_EXT=""
  if [[ "$OUTPUT_BIN" == *.* ]]; then
    OUTPUT_EXT=".${OUTPUT_BIN##*.}"
    OUTPUT_BASE="${OUTPUT_BIN%.*}"
  fi

  built_files=()
  for mf in "${MARCH_FLAGS[@]}"; do
    march_val="${mf#*=}"
    # sanitize march value for filename
    safe_march="$(echo "$march_val" | sed 's/[^A-Za-z0-9._-]/_/g')"
    bin_name="${OUTPUT_BASE}-${safe_march}${OUTPUT_EXT}"
    echo "  - Building for $march_val -> $bin_name"
    native-image --no-fallback "$mf" -jar "$APP_JAR" "$bin_name"
    built_files+=("$bin_name")
  done
fi

echo
echo "✅ Done!"
if [[ ${#MARCH_FLAGS[@]} -eq 0 ]]; then
  echo "Binary: ./$OUTPUT_BIN"
  echo "Try:    ./$OUTPUT_BIN"
else
  echo "Built binaries:"
  for f in "${built_files[@]}"; do
    echo " - ./$f"
  done
fi
