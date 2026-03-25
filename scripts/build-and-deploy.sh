#!/bin/bash

# Script para construir y desplegar la aplicación

set -euo pipefail

echo "Construyendo y desplegando la aplicación..."

# Construir el proyecto
if [ -f "backend/java/kiwi-parent/pom.xml" ]; then
    echo "Construyendo el proyecto con Maven..."
    cd backend/java/kiwi-parent
    ./mvnw clean package
    cd ../../..
fi

# Construir la imagen Docker
echo "Construyendo la imagen Docker..."
cd backend/java
make image
cd ..

# Desplegar la aplicación
echo "Desplegando la aplicación..."
# Aquí se podría integrar el despliegue a Kubernetes o Helm
# Por ahora, solo mostramos un mensaje
echo "Despliegue completado (pendiente de implementación)"
