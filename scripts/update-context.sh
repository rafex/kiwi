#!/bin/bash

# Script para actualizar la documentación de contexto automáticamente

set -euo pipefail

# Directorios
CONTEXT_DIR="context"
ARCHITECTURE_DIR="$CONTEXT_DIR/architecture"
ENV_TEMPLATES_DIR="$CONTEXT_DIR/env-templates"
CONFIG_TEMPLATES_DIR="$CONTEXT_DIR/config-templates"
DECISIONS_DIR="$CONTEXT_DIR/decisions"

# Crear directorios si no existen
mkdir -p "$ARCHITECTURE_DIR" "$ENV_TEMPLATES_DIR" "$CONFIG_TEMPLATES_DIR" "$DECISIONS_DIR"

echo "Actualizando documentación de contexto..."

# Copiar documentación existente a architecture/
if [ -d "backend/java/docs" ]; then
    echo "Copiando documentación existente a architecture/"
    cp -r backend/java/docs/* "$ARCHITECTURE_DIR/"
fi

# Copiar AGENTS.md a agents-flow.md
if [ -f "AGENTS.md" ]; then
    echo "Copiando AGENTS.md a agents-flow.md"
    cp "AGENTS.md" "$ARCHITECTURE_DIR/agents-flow.md"
fi

# Generar diagramas de dependencias desde pom.xml
if [ -f "backend/java/kiwi-parent/pom.xml" ]; then
    echo "Generando diagramas de dependencias desde pom.xml"
    cd backend/java/kiwi-parent
    mvn dependency:tree -DoutputFile=dependency-tree.txt
    if [ -f "dependency-tree.txt" ]; then
        echo "Diagrama de dependencias generado en dependency-tree.txt"
        # Crear un archivo Markdown con el árbol de dependencias
        mkdir -p "../../context/architecture"
        echo "# Árbol de Dependencias" > "../../context/architecture/dependency-tree.md"
        echo "" >> "../../context/architecture/dependency-tree.md"
        echo "\`\`\`" >> "../../context/architecture/dependency-tree.md"
        cat dependency-tree.txt >> "../../context/architecture/dependency-tree.md"
        echo "\`\`\`" >> "../../context/architecture/dependency-tree.md"
        rm dependency-tree.txt
    fi
    cd ../../..
fi

# Actualizar documentación basada en comentarios Javadoc
if [ -d "backend/java/src" ]; then
    echo "Actualizando documentación basada en Javadoc"
    cd backend/java/kiwi-parent
    mvn javadoc:javadoc -DreportOutputDirectory=target/site/apidocs
    if [ -d "target/site/apidocs" ]; then
        echo "Documentación Javadoc generada"
        # Crear directorio para Javadoc si no existe
        mkdir -p "../../context/architecture"
        # Copiar la documentación generada a context/architecture/
        cp -r target/site/apidocs "../../context/architecture/javadoc"
    fi
    cd ../../..
fi

echo "Documentación de contexto actualizada correctamente."
