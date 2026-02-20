# Kiwi

Kiwi es un sistema de almacenamiento genérico orientado a registrar objetos, ubicaciones y eventos, con backend Java modular y persistencia en PostgreSQL gestionada con Flyway.

## Objetivo del proyecto

- Proveer una base backend para gestión de inventario/objetos.
- Mantener una arquitectura limpia por módulos (puertos, core, infraestructura y transportes).
- Facilitar operación local mediante contenedor, variables de entorno y scripts de prueba.

## Licencia

Este proyecto está licenciado bajo **Apache License 2.0**.

- Texto completo: [LICENSE](LICENSE)

## Qué puedes encontrar aquí

- `backend/java`: backend Java multi-módulo (HTTP Jetty, seguridad JWT, observabilidad con Glowroot).
- `db`: migraciones y utilidades Flyway para esquema y seguridad de base de datos.
- `script/test`: scripts shell para pruebas rápidas de endpoints.
- `Makefile` raíz: utilidades para validar/exportar variables desde `.env`.

## Inicio rápido

1. Crea tu archivo de entorno desde el ejemplo:

	```bash
	cp .env.example .env
	```

2. Carga variables requeridas en tu shell:

	```bash
	eval "$(make print_env)"
	```

3. Ejecuta migraciones de base de datos:

	```bash
	cd db
	make migrate
	```

4. Construye y ejecuta el backend en contenedor:

	```bash
	cd ../backend/java
	make run-image
	```

## Índice de documentación

### Documentación principal

- [README del backend Java](backend/java/README.md)
- [Documentación técnica Java](backend/java/docs/README.md)
- [README de base de datos (Flyway)](db/README.md)

### Backend Java (módulos)

- [Agregador Maven (`kiwi-parent`)](backend/java/kiwi-parent/README.md)
- [kiwi-ports](backend/java/kiwi-parent/kiwi-ports/README.md)
- [kiwi-common](backend/java/kiwi-parent/kiwi-common/README.md)
- [kiwi-core](backend/java/kiwi-parent/kiwi-core/README.md)
- [kiwi-infra-postgres](backend/java/kiwi-parent/kiwi-infra-postgres/README.md)
- [kiwi-bootstrap](backend/java/kiwi-parent/kiwi-bootstrap/README.md)
- [kiwi-transport-jetty](backend/java/kiwi-parent/kiwi-transport-jetty/README.md)
- [kiwi-transport-grpc](backend/java/kiwi-parent/kiwi-transport-grpc/README.md)
- [kiwi-transport-rabbitmq](backend/java/kiwi-parent/kiwi-transport-rabbitmq/README.md)
- [kiwi-tools](backend/java/kiwi-parent/kiwi-tools/README.md)

## Variables de entorno clave

Definidas en [.env.example](.env.example):

- DB/Flyway: `FLYWAY_URL`, `FLYWAY_USER`, `FLYWAY_PASSWORD`, `DB_URL`, `DB_USER`, `DB_PASSWORD`
- JWT: `JWT_ISS`, `JWT_AUD`, `JWT_SECRET`, `JWT_TTL_SECONDS`
- Provisioning: `KIWI_USER`, `KIWI_PASS`, `ENVIRONMENT`, `ENABLE_USER_PROVISIONING`, `BOOTSTRAP_TOKEN`

## Pruebas rápidas

Scripts disponibles en [script/test](script/test):

- `health.sh`
- `say_hello.sh`
- `create_object.sh`
- `search.sh`
- `fuzzy.sh`
