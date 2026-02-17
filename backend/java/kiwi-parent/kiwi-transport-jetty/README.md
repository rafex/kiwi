# kiwi-transport-jetty

Transporte HTTP principal del backend usando Jetty embebido.

Depende de:

- `kiwi-bootstrap` (app ensamblada)
- Jetty server
- Jackson

Este módulo genera el fat JAR:

- `target/kiwi-transport-jetty-0.1.0-SNAPSHOT-jar-with-dependencies.jar`

## Qué contiene

- `main` de la aplicación (`dev.rafex.kiwi.App`).
- Endpoints HTTP y arranque del servidor embebido.

## Relación con Makefile

El `Makefile` de `kiwi-parent` está orientado a este módulo:

- `make exec-jetty`: ejecución en JVM (debug remoto puerto `5005`).
- `make build`: compila y empaqueta, incluyendo el fat JAR.
- `make glowroot-jetty`: ejecución con agente Glowroot.
- `make native`: construye binario nativo desde el fat JAR.
- `make image` / `make run-image`: imagen y ejecución en contenedor.

## Ejecución directa Maven

- `../mvnw -pl kiwi-transport-jetty -am exec:java`
