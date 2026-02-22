#!/bin/sh

set -eu

SOURCE_DIR="$(cd "$(dirname "$0")" && pwd)"
. "$SOURCE_DIR/common.sh"

USER_NAME="rafex"
PASSWORD="secret"

usage() {
  cat <<EOF >&2
Uso: $0 [-U USER] [-P PASSWORD] [-u BASE_URL]

  -U USER       Usuario para Basic Auth (por defecto: $USER_NAME)
  -P PASSWORD   Password para Basic Auth (por defecto: $PASSWORD)
  -u BASE_URL   URL base del servidor (por defecto: $BASE_URL)
  -h            Muestra esta ayuda
EOF
  exit 2
}

while getopts "U:P:u:h" opt; do
  case "$opt" in
    U) USER_NAME="$OPTARG" ;;
    P) PASSWORD="$OPTARG" ;;
    u) BASE_URL="$OPTARG" ;;
    h) usage ;;
    *) usage ;;
  esac
done

basic_auth="$(printf '%s:%s' "$USER_NAME" "$PASSWORD" | base64 | tr -d '\n')"
response=$(api_post_basic_auth "$basic_auth" /auth/login)

http_code=$(printf '%s' "$response" | tail -n1)
body=$(printf '%s' "$response" | sed '$d')

echo "HTTP status: $http_code"
if [ -n "$body" ]; then
  print_json "$body"
fi

if [ "$http_code" -ge 300 ]; then
  exit 1
fi

access_token=$(printf '%s' "$body" | jq -r '.access_token // empty')
token_type=$(printf '%s' "$body" | jq -r '.token_type // empty')

if [ -z "$access_token" ]; then
  echo "Error: no se encontró access_token en la respuesta." >&2
  exit 1
fi

if [ -n "$token_type" ] && [ "$token_type" != "Bearer" ]; then
  echo "Advertencia: token_type='$token_type' (esperado: Bearer)" >&2
fi

TOKEN="$access_token"
export TOKEN

echo
echo "Para reutilizar el token en esta shell:"
printf 'export TOKEN=%s\n' "$TOKEN"