#!/bin/sh

set -eu
SOURCE_DIR="$(cd "$(dirname "$0")" && pwd)"
. "$SOURCE_DIR/common.sh"

OBJECT_ID=""
TOKEN="${TOKEN:-}"

usage() {
  cat <<EOF >&2
Uso: $0 -i OBJECT_ID -T TOKEN [-u BASE_URL]

  -i OBJECT_ID  UUID del objeto (requerido)
  -T TOKEN      Token Bearer (o usar variable de entorno TOKEN)
  -u BASE_URL   URL base del servidor (por defecto: $BASE_URL)
  -h            Muestra esta ayuda
EOF
  exit 2
}

while getopts "i:T:u:h" opt; do
  case "$opt" in
    i) OBJECT_ID="$OPTARG" ;;
    T) TOKEN="$OPTARG" ;;
    u) BASE_URL="$OPTARG" ;;
    h) usage ;;
    *) usage ;;
  esac
done

if [ -z "$OBJECT_ID" ]; then
  echo "Error: OBJECT_ID requerido. Usa -i OBJECT_ID." >&2
  usage
fi

if [ -z "$TOKEN" ]; then
  echo "Error: token requerido. Usa -T TOKEN o exporta TOKEN." >&2
  usage
fi

response=$(api_get_bearer "$TOKEN" "/objects/$OBJECT_ID") || {
  echo "Error: fallo al conectarse a ${BASE_URL}" >&2
  exit 3
}

http_code=$(printf '%s' "$response" | tail -n1)
body=$(printf '%s' "$response" | sed '$d')

echo "HTTP status: $http_code"
print_json "$body"

if [ "$http_code" -ge 300 ]; then
  exit 1
fi
