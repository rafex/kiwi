# kiwi-core

Núcleo de negocio de Kiwi.

Depende de:

- `kiwi-common`
- `kiwi-ports`

## Qué contiene

- Casos de uso y lógica de dominio.
- Orquestación sobre puertos, sin depender de detalles concretos de transporte.

## Relación con Makefile

Se construye como parte del build multi-módulo desde `kiwi-parent`:

- `make build`
- `make build-without-tests`

Compilación aislada:

- `../mvnw -pl kiwi-core -am clean package`
