#!/bin/sh
# Crea un app client usando /admin/app-clients (requiere TOKEN de usuario ADMIN)

set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
. "$SCRIPT_DIR/common.sh"

TOKEN="${TOKEN:-}"
FILE="${FILE:-}"

usage() {
  cat <<EOF
Uso: $0 -T TOKEN [-f FILE] [-u BASE_URL]

Opciones:
  -T TOKEN   Bearer token de usuario ADMIN
  -f FILE    Archivo JSON con payload (default: inline de ejemplo)
  -u URL     BASE_URL

Payload JSON esperado:
  {
    "client_id": "mi-app",
    "client_secret": "mi-secreto",
    "name": "Integracion MI APP",
    "roles": ["READONLY"]
  }
EOF
}

while getopts "T:f:u:h" opt; do
  case "$opt" in
    T) TOKEN="$OPTARG" ;;
    f) FILE="$OPTARG" ;;
    u) BASE_URL="$OPTARG" ;;
    h)
      usage
      exit 0
      ;;
    *)
      usage >&2
      exit 2
      ;;
  esac
done

if [ -z "$TOKEN" ]; then
  echo "Error: token requerido. Usa -T TOKEN o exporta TOKEN." >&2
  exit 2
fi

if [ -n "$FILE" ]; then
  [ -f "$FILE" ] || {
    echo "Error: archivo no existe: $FILE" >&2
    exit 2
  }
  response=$(cat "$FILE" | api_post_json_here_bearer "$TOKEN" /admin/app-clients)
else
  response=$(printf '%s' '{"client_id":"kiwi-app-client","client_secret":"change_me","name":"Kiwi App Client","roles":["READONLY"]}' | api_post_json_here_bearer "$TOKEN" /admin/app-clients)
fi

status=$(printf '%s' "$response" | tail -n1)
body=$(printf '%s' "$response" | sed '$d')

printf '\nStatus: %s\n' "$status"
print_json "$body"

[ "$status" = "201" ] || exit 1
