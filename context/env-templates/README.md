# Plantillas de Entorno

Este directorio contiene plantillas de variables de entorno para diferentes entornos de despliegue.

## Plantillas Disponibles

- [dev.env.example](dev.env.example): Plantilla para entorno de desarrollo.
- [staging.env.example](staging.env.example): Plantilla para entorno de staging.
- [prod.env.example](prod.env.example): Plantilla para entorno de producción.

## Uso

1. Copiar la plantilla correspondiente al entorno:

```bash
cp context/env-templates/dev.env.example .env
```

2. Editar el archivo `.env` con los valores específicos del entorno.

3. Usar el archivo `.env` para configurar la aplicación:

```bash
source .env
./start-kiwi.sh
```

## Variables Comunes

Las siguientes variables son comunes a todos los entornos:

- `DB_URL`: URL de conexión a la base de datos PostgreSQL.
- `DB_USER`: Usuario de la base de datos.
- `DB_PASSWORD`: Contraseña de la base de datos.
- `JWT_SECRET`: Secreto para la firma de tokens JWT.
- `JWT_ISS`: Issuer de los tokens JWT.
- `JWT_AUD`: Audience de los tokens JWT.
- `JWT_TTL_SECONDS`: Tiempo de vida de los tokens JWT en segundos.
- `LOG_LEVEL`: Nivel de logging (DEBUG, INFO, WARN, ERROR).
- `PORT`: Puerto en el que se ejecutará la aplicación.

## Variables Específicas

- `ENVIRONMENT`: Entorno de ejecución (dev, staging, prod).
- `ENABLE_USER_PROVISIONING`: Habilitar la provisión de usuarios (true/false).
- `BOOTSTRAP_TOKEN`: Token para operaciones de bootstrap.
