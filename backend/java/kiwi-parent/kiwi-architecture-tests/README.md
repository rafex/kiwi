# kiwi-architecture-tests

Tests de arquitectura con ArchUnit para validar reglas hexagonales en `kiwi-parent`.

## Qué valida

- El core no depende de adaptadores (infraestructura, transporte o bootstrap).
- Los puertos (`kiwi-ports`) no dependen de core ni de adaptadores.
- Las utilidades compartidas (`kiwi-common`) no dependen de capas externas.
- Infraestructura no depende de transporte ni servicios del core.
- Transporte no depende de detalles internos de infraestructura.
- Bootstrap no depende de transporte.

## Ejecución

- `../mvnw -pl kiwi-architecture-tests -am test`
