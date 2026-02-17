# kiwi-ports

Define interfaces/contratos (puertos) que usan `kiwi-core` y los adaptadores de infraestructura/transporte.

## Qué contiene

- Contratos para entrada/salida de la aplicación.
- DTOs/abstracciones compartidas por capas.

## Relación con Makefile

No tiene `Makefile` propio. Se construye mediante el `Makefile` de `kiwi-parent`:

- `make build`
- `make build-without-tests`

Compilación aislada del módulo:

- `../mvnw -pl kiwi-ports -am clean package`
