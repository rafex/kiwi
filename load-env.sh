#!/usr/bin/env bash
#
# load-env.sh
# Carga variables de un archivo .env al entorno actual
#
# Uso:
#   source load-env.sh .env
#   source load-env.sh .env.example
#

set -euo pipefail

ENV_FILE="${1:-.env}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Archivo no encontrado: $ENV_FILE"
  return 1 2>/dev/null || exit 1
fi

echo "Cargando variables desde: $ENV_FILE"

while IFS='=' read -r key value; do
  # ignorar líneas vacías o comentarios
  [[ -z "$key" ]] && continue
  [[ "$key" =~ ^# ]] && continue

  # limpiar espacios
  key="$(echo "$key" | xargs)"
  value="$(echo "$value" | xargs)"

  export "$key=$value"

done < "$ENV_FILE"

echo "Variables cargadas correctamente."