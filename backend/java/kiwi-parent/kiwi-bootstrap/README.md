# kiwi-bootstrap

Módulo de composición/inicialización de la aplicación.

Depende de:

- `kiwi-common`
- `kiwi-core`
- `kiwi-infra-postgres`

## Qué contiene

- Wiring de dependencias entre core e infraestructura.
- Punto de ensamblado para que transportes (Jetty, etc.) consuman la app ya configurada.

## Relación con Makefile

No tiene `Makefile` propio; se construye dentro del flujo del parent:

- `make build`
- `make build-without-tests`

Compilación aislada:

- `../mvnw -pl kiwi-bootstrap -am clean package`
