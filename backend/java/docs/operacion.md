# Operación y ejecución

## Requisitos

- JDK compatible con **Java 21** para compilar.
- Maven Wrapper (`./mvnw`, ya incluido).
- PostgreSQL accesible por `DB_URL`.

## Build y ejecución (desde kiwi-parent)

```bash
make build
make exec-jetty
```

Opcionales:

```bash
make build-without-tests
make glowroot-jetty
make native
make image
make run-image
```

## Variables de entorno relevantes

### Aplicación HTTP / seguridad

- `PORT` (default `8080`)
- `LOG_LEVEL` (`DEBUG|INFO|WARN|ERROR`)
- `JWT_ISS` (issuer)
- `JWT_AUD` (audience)
- `JWT_SECRET` (secreto HMAC)
- `JWT_TTL_SECONDS` (default `3600`)

### Provisioning de usuarios

- `ENABLE_USER_PROVISIONING` (`true|false`)
- `ENVIRONMENT` (habilita endpoint admin en `work02|sandbox|dev`)
- `BOOTSTRAP_TOKEN` (token inicial para crear primer usuario)

### Hash de contraseñas

- `AUTH_SALT_BYTES` (mínimo 16)
- `AUTH_PBKDF2_ITERATIONS` (mínimo 10000)
- `KIWI_PASSWORD_HASH_BYTES` (default 32)

### Base de datos

- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`

## Contenedor

El `Dockerfile` copia el fat JAR de `kiwi-transport-jetty` a `/app/app.jar` y arranca con `start-kiwi.sh`.

Comportamiento operativo:

- expone puerto `8080`
- corre como usuario no-root `kiwi`
- usa `DB_URL`, `DB_USER`, `DB_PASSWORD` al runtime

## Observabilidad

- Perfilado en JVM: `make glowroot-jetty`
- Archivos de Glowroot locales: `backend/java/observability/glowroot`

## Nota de versión runtime

El compilador está configurado para Java 21, mientras que la imagen de contenedor usa `eclipse-temurin:25.0.2_10-jre`.
Eso funciona hacia adelante para bytecode Java 21, pero si quieres máxima simetría dev/prod puedes alinear también runtime a Java 21.
