# Contexto del Proyecto Kiwi

Este directorio centraliza toda la información contextual del proyecto Kiwi, incluyendo:

- **architecture/**: Diagramas arquitectónicos y documentación técnica
- **env-templates/**: Plantillas de configuración para diferentes entornos
- **config-templates/**: Configuraciones para despliegue (Helm, Kubernetes, etc.)
- **decisions/**: Registro de decisiones arquitectónicas (ADRs)

## Uso

1. **Para desarrolladores nuevos**: Comenzar con `architecture/README.md` para entender la estructura del proyecto.
2. **Para despliegues**: Usar las plantillas en `env-templates/` y `config-templates/`.
3. **Para entender decisiones técnicas**: Revisar los ADRs en `decisions/`.

## Mantenimiento

Este directorio se actualiza automáticamente mediante el script `scripts/update-context.sh`.
Para actualizar manualmente:

```bash
./scripts/update-context.sh
```

## Estructura

```
context/
├── architecture/       # Diagramas y documentación técnica
├── env-templates/      # Plantillas de variables de entorno
├── config-templates/   # Configuraciones para despliegue
├── decisions/          # Decisiones arquitectónicas (ADRs)
└── README.md           # Este archivo
```
