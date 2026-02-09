
#!/bin/sh

# Script de prueba para buscar objetos. Formatea la respuesta JSON con jq.

set -eu

SOURCE_DIR="$(cd "$(dirname "$0")" && pwd)"
. "$SOURCE_DIR/common.sh"

Q=""
TAGS=""
LOCATION_ID=""
LIMIT=""

usage() {
	cat <<EOF >&2
Uso: $0 -q QUERY [-t TAGS] [-i LOCATION_ID] [-n LIMIT] [-u BASE_URL]

  -q QUERY        Texto de búsqueda (requerido)
  -t TAGS         Lista de tags separada por comas (ej. dell,backend)
  -i LOCATION_ID  UUID de la ubicación
  -n LIMIT        Límite de resultados (int)
  -u BASE_URL     URL base del servidor (por defecto: $BASE_URL)
  -h              Muestra esta ayuda
EOF
	exit 2
}

while getopts "q:t:i:n:u:h" opt; do
	case "$opt" in
		q) Q="$OPTARG" ;;
		t) TAGS="$OPTARG" ;;
		i) LOCATION_ID="$OPTARG" ;;
		n) LIMIT="$OPTARG" ;;
		u) BASE_URL="$OPTARG" ;;
		h) usage ;;
		*) usage ;;
	esac
done

if [ -z "${Q}" ]; then
	echo "Error: el parámetro -q es obligatorio" >&2
	usage
fi

# Construir comando curl con encoding de parámetros
CURL_CMD="curl --silent --show-error --get --write-out '\n%{http_code}' --header 'Accept: application/json' --data-urlencode \"q=${Q}\""
if [ -n "${TAGS}" ]; then
	CURL_CMD="$CURL_CMD --data-urlencode \"tags=${TAGS}\""
fi
if [ -n "${LOCATION_ID}" ]; then
	CURL_CMD="$CURL_CMD --data-urlencode \"locationId=${LOCATION_ID}\""
fi
if [ -n "${LIMIT}" ]; then
	CURL_CMD="$CURL_CMD --data-urlencode \"limit=${LIMIT}\""
fi

CURL_CMD="$CURL_CMD '${BASE_URL%/}/objects/search'"

response=$(eval "$CURL_CMD") || {
	echo "Error: fallo al conectarse a ${BASE_URL}" >&2
	exit 3
}

# separar cuerpo y código HTTP
http_code=$(printf '%s' "$response" | tail -n1)
body=$(printf '%s' "$response" | sed '$d')

echo "HTTP status: $http_code"

if [ "$http_code" -ne 200 ]; then
	echo "Respuesta del servidor (no OK):" >&2
	print_json "$body"
	exit 4
fi

# Formatear salida y mostrar un resumen sencillo
print_json "$body"
echo
printf 'Resultados: %s\n' "$(printf '%s' "$body" | jq -r '.items | length')"

