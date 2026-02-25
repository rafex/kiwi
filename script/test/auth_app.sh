#!/bin/sh
# Obtiene token de aplicativo via /auth/token (client_credentials)

set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
. "$SCRIPT_DIR/common.sh"

CLIENT_ID="${CLIENT_ID:-kiwi-app-client}"
CLIENT_SECRET="${CLIENT_SECRET:-change_me}"
USE_BASIC="${USE_BASIC:-1}"

usage() {
  cat <<EOF
Uso: $0 [-i CLIENT_ID] [-s CLIENT_SECRET] [-u BASE_URL] [--no-basic]

Opciones:
  -i CLIENT_ID      Client ID (default: $CLIENT_ID)
  -s CLIENT_SECRET  Client secret (default: CLIENT_SECRET env o change_me)
  -u BASE_URL       URL base (default: $BASE_URL)
  --no-basic        Envía credenciales en body JSON (por defecto usa Basic)
EOF
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    -i)
      CLIENT_ID="$2"; shift 2 ;;
    -s)
      CLIENT_SECRET="$2"; shift 2 ;;
    -u)
      BASE_URL="$2"; shift 2 ;;
    --no-basic)
      USE_BASIC="0"; shift ;;
    -h|--help)
      usage; exit 0 ;;
    *)
      echo "Argumento no reconocido: $1" >&2
      usage
      exit 2 ;;
  esac
done

if [ "$USE_BASIC" = "1" ]; then
  basic_auth="$(printf '%s:%s' "$CLIENT_ID" "$CLIENT_SECRET" | base64 | tr -d '\n')"
  response=$(run_curl "$CURL_COMMON -X POST -H 'Authorization: Basic $basic_auth' -H 'Content-Type: application/x-www-form-urlencoded' --data 'grant_type=client_credentials' \"${BASE_URL%/}/auth/token\" --write-out '\n%{http_code}'")
else
  payload=$(printf '{"grant_type":"client_credentials","client_id":"%s","client_secret":"%s"}' "$CLIENT_ID" "$CLIENT_SECRET")
  response=$(printf '%s' "$payload" | api_post_json_here /auth/token)
fi

status=$(printf '%s' "$response" | tail -n1)
body=$(printf '%s' "$response" | sed '$d')

printf '\nStatus: %s\n' "$status"
print_json "$body"

if [ "$status" != "200" ]; then
  echo "Error autenticando app client." >&2
  exit 1
fi

access_token=$(printf '%s' "$body" | jq -r '.access_token // empty')
if [ -z "$access_token" ]; then
  echo "Error: no se encontró access_token en respuesta." >&2
  exit 1
fi

TOKEN="$access_token"
export TOKEN

echo ""
echo "Para reutilizar el token en esta shell:"
printf 'export TOKEN=%s\n' "$TOKEN"
