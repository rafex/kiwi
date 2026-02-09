#!/bin/sh
# Variables y helpers comunes para scripts de prueba

set -eu

command -v jq >/dev/null 2>&1 || {
    echo "Error: jq no está instalado. Instálalo (ej. apt install jq)" >&2
    exit 1
}

: "${BASE_URL:=http://localhost:8080}"

CURL_COMMON="curl --silent --show-error --header 'Accept: application/json'"

api_get() {
    local path="$1"; shift
    local args=()
    while [ "$#" -gt 0 ]; do
        args+=(--data-urlencode "$1")
        shift
    done
    eval "$CURL_COMMON --get ${args[@]+${args[@]}} \"${BASE_URL%/}${path}\" --write-out '\\n%{http_code}'"
}

api_post_json_here() {
    # POST leyendo JSON desde stdin
    local path="$1"
    eval "$CURL_COMMON -X POST -H 'Content-Type: application/json' --data-binary @- \"${BASE_URL%/}${path}\" --write-out '\\n%{http_code}'"
}

print_json() {
    printf '%s\n' "$1" | jq . || printf '%s\n' "$1"
}
