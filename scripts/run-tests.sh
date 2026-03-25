#!/bin/bash

# Script para ejecutar las pruebas del proyecto

set -euo pipefail

echo "Ejecución de pruebas..."

# Ejecutar pruebas con Maven
if [ -f "backend/java/kiwi-parent/pom.xml" ]; then
    echo "Ejecución de pruebas con Maven..."
    cd backend/java/kiwi-parent
    ./mvnw test
    cd ../../..
fi

echo "Pruebas completadas."
