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

## 7. CI/CD con GitHub Actions

El proyecto Kiwi usa varios flujos de trabajo definidos en `.github/workflows` para automatizar la construcción, publicación y despliegue.

### 7.1 `Java Build (Makefile)`

* **Evento desencadenador:** `push` a tags `v*.*` y `workflow_dispatch` manual.
* **Objetivo:** Compilar el backend Java, ejecutar pruebas, generar binario nativo (opcional) y publicar un release en GitHub.
* **Jobs:**
  * `check_paths` – verifica si hay cambios en `backend/java/kiwi*`.
  * `build` – compila con Maven (`make build`), opcionalmente crea binario nativo con GraalVM, valida y publica el release usando `softprops/action-gh-release`.
* **Herramientas:** Maven, Makefile, GraalVM, `softprops/action-gh-release`.

### 7.2 `Build and Publish Container`

* **Evento desencadenador:** `push` a la rama `main`, tags `v*.*` y `workflow_dispatch`.
* **Objetivo:** Construir la imagen Docker del backend y subirla a GitHub Container Registry (GHCR).
* **Jobs:**
  * `check_paths` – detecta cambios en `backend/java/kiwi*` o `backend/java/Dockerfile`.
  * `build-and-push` – compila con Maven, configura QEMU y Buildx, y ejecuta `docker/build-push-action` para generar imágenes para `amd64` y `arm64`. Etiqueta `latest` o el tag de versión.
* **Herramientas:** Maven, Docker, QEMU, Buildx, GHCR.

### 7.3 `Deploy Kiwi Backend`

* **Evento desencadenador:** `workflow_dispatch` manual (con input `version_tag`) y `push` de tags `v*.*` que afectan el chart Helm.
* **Objetivo:** Desplegar la imagen Docker en un clúster K3s mediante Helm.
* **Jobs:**
  * `deploy-backend` – configura kubeconfig, valida secret, lint del chart, ejecuta `helm upgrade --install`, verifica rollout y sube artefactos de diagnóstico.
* **Entorno:** Despliegue en namespace `mvps` usando Helm chart `helm/kiwi-backend`. La imagen proviene de GHCR (`ghcr.io/rafex/kiwi-jetty-backend`).

### 7.4 Resumen de la Cadena CI/CD

1. **Código push/tag** → dispara `Java Build` y/o `Build and Publish Container`.
2. **Imagen Docker** disponible en GHCR.
3. **Deploy manual o por tag** → `Deploy Kiwi Backend` actualiza el clúster K3s.
4. Cada flujo usa **Maven**, **Makefile**, **Docker**, **Helm** y **GraalVM** según corresponda.

> **Nota:** Los flujos están diseñados para ejecutarse de forma aislada; los jobs `check_paths` evitan builds innecesarios cuando no hay cambios relevantes.
