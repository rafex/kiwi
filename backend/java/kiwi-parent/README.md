# kiwi-parent

Agregador Maven del backend Java de Kiwi.

Este proyecto define:

- La estructura multi-módulo.
- Versiones de dependencias y plugins comunes (`dependencyManagement` y `pluginManagement`).
- El `Makefile` principal para build, nativo y contenedor.

## Módulos

- `kiwi-ports`: contratos/puertos.
- `kiwi-common`: utilidades y tipos compartidos.
- `kiwi-core`: lógica de dominio/casos de uso.
- `kiwi-infra-postgres`: adaptador PostgreSQL.
- `kiwi-bootstrap`: wiring/composición de dependencias.
- `kiwi-transport-jetty`: transporte HTTP con Jetty embebido.
- `kiwi-transport-grpc`: placeholder para transporte gRPC.
- `kiwi-transport-rabbitmq`: placeholder para transporte RabbitMQ.
- `kiwi-tools`: utilidades ejecutables de soporte.

## Documentación técnica

Consulta la documentación central del backend Java en `../docs`:

- `../docs/README.md`

## Cómo funciona el Makefile

El `Makefile` de esta carpeta orquesta el flujo principal sobre el módulo ejecutable `kiwi-transport-jetty`.

Comandos principales:

- `make build`: compila todos los módulos con tests (`mvn clean package`).
- `make build-without-tests`: compila sin tests.
- `make exec-jetty`: ejecuta en JVM con `exec:java` y debug remoto en `5005`.
- `make glowroot-jetty`: corre el fat JAR con agente Glowroot.
- `make native`: genera binario nativo (`kiwi-jetty.bin` por defecto).
- `make native MARCH="x86-64-v2 x86-64-v3"`: genera variantes por arquitectura.
- `make image`: construye imagen de contenedor con `Dockerfile`.
- `make run-image`: levanta contenedor (soporta `DB_USER`, `DB_PASSWORD`, `DB_URL`).
- `make run-native`: ejecuta el binario nativo.

## Flujo sugerido

1. `make build`
2. `make exec-jetty` (desarrollo JVM) **o** `make native` (binario nativo)
3. `make image` y `make run-image` para despliegue local con contenedor
