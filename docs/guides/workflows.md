# Flujo de Trabajo de Kiwi

Este documento describe el flujo de trabajo típico para desarrollar y ejecutar todo el stack del proyecto Kiwi, que incluye el **backend Java**, la **base de datos** y el **cliente API (OpenAPI Node Client)**.

## 1. Preparación del entorno

```bash
# Cargar variables de entorno desde .env
eval "$(make load-env)"
```

## 2. Compilación y pruebas del backend

```bash
# Compilar el backend (sin tests) – rápido para iteraciones
make backend-build

# Ejecutar pruebas y chequeos de calidad
make backend-quality
```

## 3. Base de datos

```bash
# Aplicar migraciones con Flyway
make db-migrate
```

## 4. Cliente API (OpenAPI Node Client)

El cliente es una UI web generada a partir del spec OpenAPI y permite explorar los endpoints.

```bash
# Instalar dependencias (solo una vez)
make client-install

# Ejecutar el cliente (servidor web) desde la raíz del repositorio
make client-run
```

El cliente se expone en `http://localhost:3030` (puerto configurable vía `KIWI_CLIENT_PORT`).

## 5. Ejecutar la aplicación completa (Docker)

```bash
# Construir la imagen Docker del backend
make backend-image

# Ejecutar la imagen con variables de entorno
make backend-run-image
```

Con la base de datos y el cliente corriendo, podrás probar la API completa.

## 6. Limpieza

```bash
# Detener contenedores y limpiar la base de datos (cuidado)
make db-clean
```

---

> **Nota:** Todos los comandos anteriores pueden ejecutarse desde la raíz del proyecto gracias al *Makefile* principal que delega en los *Makefiles* internos.
