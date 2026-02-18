# kiwi-tools

Módulo de utilidades ejecutables para tareas operativas y soporte.

## Dependencias

- `kiwi-core`
- `kiwi-infra-postgres`

## Punto de entrada

Define `main.class=dev.rafex.kiwi.tools.Main` en su `pom.xml`.

## Relación con Makefile

No tiene targets dedicados en el `Makefile` principal, pero se compila dentro del build multi-módulo:

- `make build`
- `make build-without-tests`

Compilación/ejecución aislada:

- `../mvnw -pl kiwi-tools -am clean package`
- `../mvnw -pl kiwi-tools -am exec:java`
