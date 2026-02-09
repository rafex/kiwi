#!/bin/sh

set -eu
SOURCE_DIR="$(cd "$(dirname "$0")" && pwd)"
. "$SOURCE_DIR/common.sh"

response=$(eval "$CURL_COMMON --get --write-out '\n%{http_code}' \"${BASE_URL%/}/hello\"")
http_code=$(printf '%s' "$response" | tail -n1)
body=$(printf '%s' "$response" | sed '$d')

echo "HTTP status: $http_code"
print_json "$body"

if [ "$http_code" -ge 300 ]; then
	exit 1
fi
