# kiwi-common

Módulo de utilidades y piezas compartidas entre el resto de módulos.

## Qué contiene

- Tipos base reutilizables.
- Helpers comunes sin acoplarse a infraestructura específica.

## Relación con Makefile

No tiene `Makefile` propio. Se compila desde el `Makefile` de `kiwi-parent`:

- `make build`
- `make build-without-tests`

Si solo quieres compilar este módulo:

- `../mvnw -pl kiwi-common -am clean package`
