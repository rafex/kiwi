# Backend Java (Kiwi)

Este directorio contiene el backend Java multi-módulo de Kiwi.

## Índice principal

- Agregador Maven: [kiwi-parent](kiwi-parent/README.md)
- Documentación técnica central: [docs](docs/README.md)

## README por módulo

- [kiwi-parent](kiwi-parent/README.md)
- [kiwi-parent/kiwi-ports](kiwi-parent/kiwi-ports/README.md)
- [kiwi-parent/kiwi-common](kiwi-parent/kiwi-common/README.md)
- [kiwi-parent/kiwi-core](kiwi-parent/kiwi-core/README.md)
- [kiwi-parent/kiwi-infra-postgres](kiwi-parent/kiwi-infra-postgres/README.md)
- [kiwi-parent/kiwi-bootstrap](kiwi-parent/kiwi-bootstrap/README.md)
- [kiwi-parent/kiwi-transport-jetty](kiwi-parent/kiwi-transport-jetty/README.md)
- [kiwi-parent/kiwi-transport-grpc](kiwi-parent/kiwi-transport-grpc/README.md)
- [kiwi-parent/kiwi-transport-rabbitmq](kiwi-parent/kiwi-transport-rabbitmq/README.md)
- [kiwi-parent/kiwi-tools](kiwi-parent/kiwi-tools/README.md)

## Observabilidad

- Glowroot local: [observability/glowroot](observability/glowroot)

## Inicio rápido

1. Ve a `backend/java/kiwi-parent`
2. Ejecuta `make build`
3. Ejecuta `make exec-jetty`
