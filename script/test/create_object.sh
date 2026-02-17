#!/bin/sh

set -eu
SOURCE_DIR="$(cd "$(dirname "$0")" && pwd)"
. "$SOURCE_DIR/common.sh"

FILE="$SOURCE_DIR/object.json"
TOKEN="${TOKEN:-}"

usage() {
  cat <<EOF >&2
Uso: $0 -T TOKEN [-f FILE]

  -f FILE   Archivo JSON con el objeto a crear (por defecto: $FILE)
  -T TOKEN  Token Bearer (o usar variable de entorno TOKEN)
  -h        Muestra esta ayuda
EOF
  exit 2
}

while getopts "f:T:h" opt; do
  case "$opt" in
    f) FILE="$OPTARG" ;;
    T) TOKEN="$OPTARG" ;;
    h) usage ;;
    *) usage ;;
  esac
done

if [ -z "$TOKEN" ]; then
  echo "Error: token requerido. Usa -T TOKEN o exporta TOKEN." >&2
  usage
fi

if [ ! -f "$FILE" ]; then
  echo "Error: archivo de JSON no encontrado: $FILE" >&2
  exit 3
fi

response=$(cat "$FILE" | api_post_json_here_bearer "$TOKEN" /objects)

http_code=$(printf '%s' "$response" | tail -n1)
body=$(printf '%s' "$response" | sed '$d')

echo "HTTP status: $http_code"
print_json "$body"

if [ "$http_code" -ge 300 ]; then
  exit 1
fi
