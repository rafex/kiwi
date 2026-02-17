
#!/bin/sh

# Script de prueba para buscar objetos. Formatea la respuesta JSON con jq.

set -eu

SOURCE_DIR="$(cd "$(dirname "$0")" && pwd)"
. "$SOURCE_DIR/common.sh"

Q=""
TAGS=""
LOCATION_ID=""
LIMIT=""
TOKEN="${TOKEN:-}"

usage() {
	cat <<EOF >&2
Uso: $0 -q QUERY -T TOKEN [-t TAGS] [-i LOCATION_ID] [-n LIMIT] [-u BASE_URL]

  -q QUERY        Texto de búsqueda (requerido)
	-T TOKEN        Token Bearer (o usar variable de entorno TOKEN)
  -t TAGS         Lista de tags separada por comas (ej. dell,backend)
  -i LOCATION_ID  UUID de la ubicación
  -n LIMIT        Límite de resultados (int)
  -u BASE_URL     URL base del servidor (por defecto: $BASE_URL)
  -h              Muestra esta ayuda
EOF
	exit 2
}

while getopts "q:t:i:n:u:T:h" opt; do
	case "$opt" in
		q) Q="$OPTARG" ;;
		t) TAGS="$OPTARG" ;;
		i) LOCATION_ID="$OPTARG" ;;
		n) LIMIT="$OPTARG" ;;
		u) BASE_URL="$OPTARG" ;;
		T) TOKEN="$OPTARG" ;;
		h) usage ;;
		*) usage ;;
	esac
done

if [ -z "${Q}" ]; then
	echo "Error: el parámetro -q es obligatorio" >&2
	usage
fi

if [ -z "$TOKEN" ]; then
	echo "Error: token requerido. Usa -T TOKEN o exporta TOKEN." >&2
	usage
fi

if [ -n "${TAGS}" ] && [ -n "${LOCATION_ID}" ] && [ -n "${LIMIT}" ]; then
	response=$(api_get_bearer "$TOKEN" /objects/search "q=${Q}" "tags=${TAGS}" "locationId=${LOCATION_ID}" "limit=${LIMIT}") || {
		echo "Error: fallo al conectarse a ${BASE_URL}" >&2
		exit 3
	}
elif [ -n "${TAGS}" ] && [ -n "${LOCATION_ID}" ]; then
	response=$(api_get_bearer "$TOKEN" /objects/search "q=${Q}" "tags=${TAGS}" "locationId=${LOCATION_ID}") || {
		echo "Error: fallo al conectarse a ${BASE_URL}" >&2
		exit 3
	}
elif [ -n "${TAGS}" ] && [ -n "${LIMIT}" ]; then
	response=$(api_get_bearer "$TOKEN" /objects/search "q=${Q}" "tags=${TAGS}" "limit=${LIMIT}") || {
		echo "Error: fallo al conectarse a ${BASE_URL}" >&2
		exit 3
	}
elif [ -n "${LOCATION_ID}" ] && [ -n "${LIMIT}" ]; then
	response=$(api_get_bearer "$TOKEN" /objects/search "q=${Q}" "locationId=${LOCATION_ID}" "limit=${LIMIT}") || {
		echo "Error: fallo al conectarse a ${BASE_URL}" >&2
		exit 3
	}
elif [ -n "${TAGS}" ]; then
	response=$(api_get_bearer "$TOKEN" /objects/search "q=${Q}" "tags=${TAGS}") || {
		echo "Error: fallo al conectarse a ${BASE_URL}" >&2
		exit 3
	}
elif [ -n "${LOCATION_ID}" ]; then
	response=$(api_get_bearer "$TOKEN" /objects/search "q=${Q}" "locationId=${LOCATION_ID}") || {
		echo "Error: fallo al conectarse a ${BASE_URL}" >&2
		exit 3
	}
elif [ -n "${LIMIT}" ]; then
	response=$(api_get_bearer "$TOKEN" /objects/search "q=${Q}" "limit=${LIMIT}") || {
		echo "Error: fallo al conectarse a ${BASE_URL}" >&2
		exit 3
	}
else
	response=$(api_get_bearer "$TOKEN" /objects/search "q=${Q}") || {
		echo "Error: fallo al conectarse a ${BASE_URL}" >&2
		exit 3
	}
fi

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

