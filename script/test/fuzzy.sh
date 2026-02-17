
#!/bin/sh

# Script de prueba para buscar objetos por nombre (fuzzy). Formatea la respuesta JSON con jq.

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
Uso: $0 -q NAME -T TOKEN [-n LIMIT] [-u BASE_URL]

	-q NAME         Texto para búsqueda fuzzy en el campo `name` (requerido)
	-T TOKEN        Token Bearer (o usar variable de entorno TOKEN)
	-n LIMIT        Límite de resultados (int)
	-u BASE_URL     URL base del servidor (por defecto: $BASE_URL)
	-h              Muestra esta ayuda
EOF
	exit 2
}

while getopts "q:n:u:T:h" opt; do
	case "$opt" in
		q) Q="$OPTARG" ;;
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

if [ -n "${LIMIT}" ]; then
	response=$(api_get_bearer "$TOKEN" /objects/fuzzy "name=${Q}" "limit=${LIMIT}") || {
		echo "Error: fallo al conectarse a ${BASE_URL}" >&2
		exit 3
	}
else
	response=$(api_get_bearer "$TOKEN" /objects/fuzzy "name=${Q}") || {
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

