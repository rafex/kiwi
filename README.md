## Actualizaciones recientes

Se han realizado las siguientes actualizaciones en el proyecto Kiwi:
- Se ha eliminado el archivo `backend/java/docs/model.md`. La información contenida en este archivo ahora se encuentra en otros lugares de la documentación.
- Se han actualizado las versiones de las dependencias en `pom.xml`: Jetty 12.1.7, Jackson 2.21.2 y los módulos Ether a la versión 8.1.0.

## Notas de migración

Al actualizar a las nuevas versiones, asegúrate de revisar la documentación de cada dependencia para conocer los cambios y mejoras realizados. En particular, la actualización de Jetty y Jackson puede requerir ajustes en la configuración y el código.

## Configuración

Kiwi ahora usa **Ether‑Config** para la gestión centralizada de configuración. La configuración se carga desde variables de entorno, propiedades del sistema y (opcionalmente) archivos YAML.

- [CONFIGURATION.md](backend/java/docs/CONFIGURATION.md) – Guía completa de configuración
- [MIGRATION.md](backend/java/docs/MIGRATION.md) – Guía de migración desde el sistema anterior

## Documentación

La documentación del proyecto Kiwi se encuentra en el directorio `docs/`. Para obtener más información sobre la arquitectura y el diseño del proyecto, consulta los archivos `docs/architecture.md` y `docs/design.md`. La información sobre la eliminación del archivo `model.md` y su reubicación se encuentra en `docs/changes.md`.

## Dependencias

Las dependencias del proyecto Kiwi se encuentran en el archivo `pom.xml`. Asegúrate de actualizar las versiones de las dependencias según sea necesario.

## Listado de archivos de documentación

A continuación se enumeran todos los archivos de documentación presentes en el repositorio:

- /Users/rafex/repository/github/rafex/kiwi/README.md
- /Users/rafex/repository/github/rafex/kiwi/backend/java/docs/arquitectura.md
- /Users/rafex/repository/github/rafex/kiwi/backend/java/docs/query_handling.md
- /Users/rafex/repository/github/rafex/kiwi/backend/java/docs/operacion.md
- /Users/rafex/repository/github/rafex/kiwi/openapi/node-client/README.md
- /Users/rafex/repository/github/rafex/kiwi/Analysis.md
- /Users/rafex/repository/github/rafex/kiwi/AGENTS.md
- /Users/rafex/repository/github/rafex/kiwi/backend/java/kiwi-parent/README.md
- /Users/rafex/repository/github/rafex/kiwi/backend/java/kiwi-parent/kiwi-architecture-tests/README.md
- /Users/rafex/repository/github/rafex/kiwi/helm/kiwi-backend/README.md
- /Users/rafex/repository/github/rafex/kiwi/backend/java/docs/README.md
- /Users/rafex/repository/github/rafex/kiwi/backend/java/docs/CONFIGURATION.md
- /Users/rafex/repository/github/rafex/kiwi/backend/java/docs/MIGRATION.md
- /Users/rafex/repository/github/rafex/kiwi/backend/java/kiwi-parent/kiwi-tools/README.md
- /Users/rafex/repository/github/rafex/kiwi/backend/java/README.md
- /Users/rafex/repository/github/rafex/kiwi/backend/java/kiwi-parent/kiwi-transport-rabbitmq/README.md
- /Users/rafex/repository/github/rafex/kiwi/backend/java/kiwi-parent/kiwi-transport-grpc/README.md
- /Users/rafex/repository/github/rafex/kiwi/backend/java/kiwi-parent/kiwi-transport-jetty/README.md
- /Users/rafex/repository/github/rafex/kiwi/backend/java/kiwi-parent/kiwi-bootstrap/README.md
- /Users/rafex/repository/github/rafex/kiwi/backend/java/kiwi-parent/kiwi-infra-postgres/README.md
- /Users/rafex/repository/github/rafex/kiwi/backend/java/kiwi-parent/kiwi-ports/README.md
- /Users/rafex/repository/github/rafex/kiwi/backend/java/kiwi-parent/kiwi-core/README.md
- /Users/rafex/repository/github/rafex/kiwi/backend/java/kiwi-parent/kiwi-common/README.md
- /Users/rafex/repository/github/rafex/kiwi/db/README.md

## Makefile de proyecto

El proyecto Kiwi utiliza un **Makefile único en la raíz** que delega en Makefiles específicos de cada sub‑módulo. Esta arquitectura permite ejecutar tareas comunes desde la raíz sin conocer la ubicación interna de los sub‑makefiles.

### Estructura

- `backend/java/Makefile` – tareas relacionadas con contenedores Docker.
- `backend/java/kiwi-parent/Makefile` – compilación, pruebas y generación de imágenes nativas del backend Java.
- `db/Makefile` – gestión de migraciones y mantenimiento de la base de datos.

### Targets disponibles desde la raíz

#### Gestión de entorno
- `print_env` – Imprime comandos `export` para cargar variables de `.env`.
- `load-env` – Genera un script `set -a && source .env && set +a`.
- `install-githooks` – Configura la ruta de los hooks de Git.

#### Release
- `print-next-tag` – Calcula la siguiente etiqueta de versión.
- `release-tag` – Crea y push de la nueva etiqueta.

#### Backend Java
- `backend-help` – Muestra los targets del backend.
- `backend-build` – Compila con Maven (sin tests).
- `backend-quality` – Ejecuta checks de calidad.
- `backend-agent` – Ejecuta con `native-image-agent`.
- `backend-native` – Genera una imagen nativa.
- `backend-run-native` – Ejecuta el binario nativo.

#### Contenedores
- `backend-image` – Construye la imagen Docker del backend.
- `backend-run-image` – Ejecuta la imagen con variables de entorno.
- `backend-run-publish-image` – Ejecuta la imagen publicada en GHCR.

#### Base de datos
- `db-help` – Muestra los targets de DB.
- `db-migrate` – Ejecuta migraciones Flyway.
- `db-info` – Estado de migraciones.
- `db-validate` – Valida migraciones.
- `db-repair` – Repara metadatos de Flyway.
- `db-clean` – Elimina la base de datos (¡CUIDADO!).

### Ejemplos de uso comunes

```bash
# Cargar variables de entorno en la sesión actual
eval "$(make load-env)"

# Compilar el backend Java
make backend-build

# Ejecutar la aplicación en Docker
make backend-run-image

# Aplicar migraciones de base de datos
make db-migrate
# Ejecutar cliente API
make client-run

# Crear y publicar una nueva etiqueta de release
make release-tag
```

## Contribución

Para contribuir al proyecto Kiwi, asegúrate de seguir las instrucciones de contribución en `docs/contributing.md`. Esto incluye información sobre cómo enviar solicitudes de extracción y cómo reportar errores.
# Test commit para activar workflow
# Test pipeline - viernes, 27 de marzo de 2026, 13:48:42 CST
