#!/bin/sh

set -eu
SOURCE_DIR="$(cd "$(dirname "$0")" && pwd)"
. "$SOURCE_DIR/common.sh"

NAME="${NAME:-}"

if [ -n "$NAME" ]; then
	response=$(api_get /hello "name=${NAME}")
else
	response=$(api_get /hello)
fi
http_code=$(printf '%s' "$response" | tail -n1)
body=$(printf '%s' "$response" | sed '$d')

echo "HTTP status: $http_code"
print_json "$body"

if [ "$http_code" -ge 300 ]; then
	exit 1
fi

printf '%s\n' "$body" | jq -e '.message | type=="string"' >/dev/null
printf '%s\n' "$body" | jq -e '.version | type=="string"' >/dev/null
