# kiwi-transport-grpc

Módulo reservado para transporte gRPC.

Actualmente su `pom.xml` está en estado base y funciona como placeholder para la implementación futura.

## Objetivo del módulo

- Exponer capacidades de `kiwi-core` por gRPC.
- Mantener desacoplada la lógica de negocio del protocolo de transporte.

## Relación con Makefile

Aunque no tiene objetivos específicos en el `Makefile`, se compila dentro del build multi-módulo:

- `make build`
- `make build-without-tests`

Compilación aislada:

- `../mvnw -pl kiwi-transport-grpc -am clean package`
