#!/bin/sh
# Variables y helpers comunes para scripts de prueba

set -eu

command -v jq >/dev/null 2>&1 || {
    echo "Error: jq no está instalado. Instálalo (ej. apt install jq)" >&2
    exit 1
}

: "${BASE_URL:=https://kiwi.v1.rafex.cloud/}"
#: "${BASE_URL:=http://localhost:8080/}"

CURL_COMMON="curl --silent --show-error --header 'Accept: application/json'"
TOKEN="${TOKEN:-}"

mask_sensitive_headers() {
    printf '%s' "$1" \
        | sed -E "s/(Authorization: Bearer )[^']+/\1***REDACTED***/g" \
        | sed -E "s/(Authorization: Basic )[^']+/\1***REDACTED***/g"
}

run_curl() {
    cmd="$1"
    safe_cmd=$(mask_sensitive_headers "$cmd")
    printf '>> curl: %s\n' "$safe_cmd" >&2
    eval "$cmd"
}

api_get() {
    path="$1"; shift
    args=""
    while [ "$#" -gt 0 ]; do
        args="$args --data-urlencode '$1'"
        shift
    done
    run_curl "$CURL_COMMON --get $args \"${BASE_URL%/}${path}\" --write-out '\\n%{http_code}'"
}

api_post_json_here() {
    # POST leyendo JSON desde stdin
    path="$1"
    run_curl "$CURL_COMMON -X POST -H 'Content-Type: application/json' --data-binary @- \"${BASE_URL%/}${path}\" --write-out '\\n%{http_code}'"
}

api_get_bearer() {
    # GET con Bearer token, parámetros opcionales por query string
    token="$1"
    path="$2"
    shift 2
    args=""
    while [ "$#" -gt 0 ]; do
        args="$args --data-urlencode '$1'"
        shift
    done
    run_curl "$CURL_COMMON --header 'Authorization: Bearer $token' --get $args \"${BASE_URL%/}${path}\" --write-out '\\n%{http_code}'"
}

api_post_json_here_bearer() {
    # POST con Bearer token leyendo JSON desde stdin
    token="$1"
    path="$2"
    run_curl "$CURL_COMMON -X POST -H 'Authorization: Bearer $token' -H 'Content-Type: application/json' --data-binary @- \"${BASE_URL%/}${path}\" --write-out '\\n%{http_code}'"
}

api_post_basic_auth() {
    basic_auth="$1"
    path="$2"
    run_curl "$CURL_COMMON -X POST -H 'Authorization: Basic $basic_auth' \"${BASE_URL%/}${path}\" --write-out '\\n%{http_code}'"
}

api_bearer() {
    # Wrapper por método: GET|POST
    # - GET:  api_bearer GET TOKEN /path ["k=v" ...]
    # - POST: printf '%s' '{"a":1}' | api_bearer POST TOKEN /path
    method="$1"
    token="$2"
    path="$3"
    shift 3

    case "$method" in
        GET|get)
            api_get_bearer "$token" "$path" "$@"
            ;;
        POST|post)
            api_post_json_here_bearer "$token" "$path"
            ;;
        *)
            echo "Error: método no soportado en api_bearer: $method (usa GET o POST)" >&2
            return 2
            ;;
    esac
}

print_json() {
    printf '%s\n' "$1" | jq . || printf '%s\n' "$1"
}
