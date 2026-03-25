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

## Contribución

Para contribuir al proyecto Kiwi, asegúrate de seguir las instrucciones de contribución en `docs/contributing.md`. Esto incluye información sobre cómo enviar solicitudes de extracción y cómo reportar errores.
