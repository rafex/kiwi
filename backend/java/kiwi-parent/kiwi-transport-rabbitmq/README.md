# kiwi-transport-rabbitmq

Módulo reservado para integración/eventos con RabbitMQ.

Actualmente su `pom.xml` está en estado base y funciona como placeholder para la implementación futura.

## Objetivo del módulo

- Publicar/consumir eventos de la aplicación.
- Implementar adaptadores de mensajería sin acoplar `kiwi-core` al broker.

## Relación con Makefile

No tiene targets dedicados en el `Makefile`, pero se construye en el build multi-módulo:

- `make build`
- `make build-without-tests`

Compilación aislada:

- `../mvnw -pl kiwi-transport-rabbitmq -am clean package`
