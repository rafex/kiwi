# kiwi-infra-postgres

Adaptador de persistencia para PostgreSQL.

Depende de:

- `kiwi-common`
- `kiwi-ports`
- PostgreSQL JDBC
- HikariCP

## Qué contiene

- Implementaciones concretas de puertos para acceso a datos en PostgreSQL.
- Configuración/uso de conexiones JDBC.

## Relación con Makefile

Se compila desde el `Makefile` de `kiwi-parent` dentro del build multi-módulo:

- `make build`
- `make build-without-tests`

Compilación aislada:

- `../mvnw -pl kiwi-infra-postgres -am clean package`
