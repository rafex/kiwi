# Scripts de Automatización

Este directorio contiene scripts para automatizar tareas comunes en el proyecto Kiwi.

## Scripts Disponibles

- [update-context.sh](update-context.sh): Actualiza la documentación de contexto automáticamente.
- [build-and-deploy.sh](build-and-deploy.sh): Construye y despliega la aplicación.
- [run-tests.sh](run-tests.sh): Ejecuta las pruebas del proyecto.

## Uso

Para ejecutar un script:

```bash
./scripts/<nombre-del-script>.sh
```

## Crear un Nuevo Script

Para crear un nuevo script:

1. Crear un nuevo archivo con extensión `.sh`.
2. Asegurarse de que el script tenga permisos de ejecución:

```bash
chmod +x scripts/<nombre-del-script>.sh
```

3. Documentar el script en este archivo `README.md`.
