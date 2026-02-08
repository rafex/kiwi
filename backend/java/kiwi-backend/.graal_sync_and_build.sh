#!/usr/bin/env bash
set -euo pipefail

JAR=""
GRAAL_CONFIG_DIR="graal-config"
RES_DIR="src/main/resources/META-INF/native-image"
MVN="./mvnw"

usage() {
  cat <<USAGE
Usage:
  graal_sync_and_build.sh --jar PATH [--graal-config DIR] [--res-dir DIR] [--mvn CMD]

What it does:
  - Copies reachability-metadata.json and/or META-INF/native-image tree
    from --graal-config into --res-dir (and parent META-INF).
  - Rebuilds the jar so metadata is packaged into the artifact.
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --jar) JAR="$2"; shift 2 ;;
    --graal-config) GRAAL_CONFIG_DIR="$2"; shift 2 ;;
    --res-dir) RES_DIR="$2"; shift 2 ;;
    --mvn) MVN="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown arg: $1"; usage; exit 2 ;;
  esac
done

[[ -n "$JAR" ]] || { echo "Missing --jar"; usage; exit 2; }
[[ -f "$JAR" ]] || { echo "Jar not found: $JAR"; exit 1; }

# Ensure directories
mkdir -p "$RES_DIR"

# Copy reachability metadata (new agent output)
if [[ -f "$GRAAL_CONFIG_DIR/reachability-metadata.json" ]]; then
  cp -f "$GRAAL_CONFIG_DIR/reachability-metadata.json" "$RES_DIR/"
  echo "[sync] Copied reachability-metadata.json -> $RES_DIR/"
fi

# Copy legacy layout if exists
# graal-config/META-INF/native-image/...
if [[ -d "$GRAAL_CONFIG_DIR/META-INF/native-image" ]]; then
  # RES_DIR is .../META-INF/native-image, so parent is .../META-INF
  cp -a "$GRAAL_CONFIG_DIR/META-INF/native-image" "$(dirname "$RES_DIR")"/
  echo "[sync] Copied META-INF/native-image/* -> $(dirname "$RES_DIR")/"
fi

echo "[sync] Current packaged metadata files (in resources):"
find "$(dirname "$RES_DIR")" -maxdepth 6 -type f -name "*.json" -print || true

# Rebuild so resources are included
echo "[sync] Rebuilding with Maven to include metadata..."
"$MVN" -q -DskipTests package
