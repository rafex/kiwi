# Plan de integración de Ether‑Config y mejora de logging en Kiwi

## 📋 Estado actual
- **Logging**: `ether-logging-core` ya está integrado mediante la clase `dev.rafex.kiwi.logging.Log`.
- **Configuración**: Uso disperso de `System.getenv()` (15 lugares) y `ServerConfig`/`KiwiContainer.KiwiConfig` parcial.
- **Problemas**: Falta de tipado, validación, fuentes múltiples y logging sin MDC.

---

## 🎯 Objetivo
1. Centralizar toda la configuración usando **ether‑config**.
2. Eliminar los accesos directos a `System.getenv()`.
3. Tipar y validar la configuración (records, anotaciones).
4. Mejorar el logging: MDC, niveles por paquete, formato estructurado.
5. Mantener compatibilidad (constructores deprecated).

---

## 🗂️ Fases y tareas

### Fase 1 – Análisis y diseño (1 día)
- Inventario completo de variables de entorno.
- Diseño de `KiwiConfig` con records anidados:
  - `DatabaseConfig`
  - `JwtConfig`
  - `AuthConfig`
  - `ServerConfig`
  - `LoggingConfig`
- Definir anotaciones de validación (`@Required`, `@Min`, `@Max`).
- Decidir orden de fuentes: **env → system‑props → YAML (opcional)**.

### Fase 2 – Implementación de `KiwiConfig` (2 días)
- **pom.xml**: añadir dependencia `ether-config` a `kiwi-common`.
- Crear paquete `dev.rafex.kiwi.config`.
- Implementar los records con binding y validación.
- Método estático `KiwiConfig load()` que compone `EtherConfig` con:
  ```java
  EtherConfig.of(
      new EnvironmentConfigSource(),
      new SystemPropertyConfigSource(),
      new YamlFileConfigSource(Path.of("config/application.yaml"))
  );
  ```
- Tests unitarios de binding y validación.

### Fase 3 – Refactorización de `Db` (1 día)
- Eliminar `System.getenv()`.
- Añadir método `Db.init(KiwiConfig cfg)` que configure `HikariConfig` usando `cfg.database()`.
- Mantener API estática (`dataSource()`, `databaseClient()`).
- Validar pool‑size, timeouts mediante `DatabaseConfig`.

### Fase 4 – Unificar `ServerConfig` y `KiwiContainer.KiwiConfig` (1 día)
- Integrar los campos actuales de `ServerConfig` dentro de `KiwiConfig.server`.
- Añadir `jwtAppTtlSeconds` y `jwtTtlSeconds` a `JwtConfig`.
- Actualizar `KiwiContainer` para usar `KiwiConfig` en vez de su record interno.
- Modificar `KiwiServer` para recibir `KiwiConfig` y crear `KiwiJwtService` con los nuevos valores.

### Fase 5 – Refactorización de handlers y servicios (2 días)
- **TokenHandler / LoginHandler**: recibir TTLs vía constructor (ya existen) y pasarles desde `DefaultKiwiModule` usando `config.jwt()`.
- **UserProvisioningServiceImpl** y **AppClientAuthServiceImpl**: recibir `AuthConfig` (salt, iterations) vía constructor.
- **CreateUserHandler**: leer `bootstrapToken` desde `config.auth().bootstrapToken()`.
- Mantener constructores actuales (deprecated) para compatibilidad.

### Fase 6 – Mejora de logging (1 día)
- Exponer MDC en `Log` (si `EtherLog` lo permite) para `requestId` y `user`.
- Añadir `LoggingConfig` con nivel por paquete y formato (texto/JSON).
- `App.configureLogging` usará `config.logging()`.

### Fase 7 – Tests y validación (1 día)
- Actualizar tests para usar `MapConfigSource` y validar carga de `KiwiConfig`.
- Verificar que los defaults sean idénticos a los actuales.
- Ejecutar pruebas de integración (start‑kiwi.sh) con variables de entorno simuladas.

### Fase 8 – Documentación (0.5 día)
- `docs/CONFIGURATION.md`: variables, defaults, fuentes.
- `docs/MIGRATION.md`: cómo migrar código existente.
- Javadoc de `KiwiConfig` con ejemplos de uso.
- Actualizar `README.md` con referencia al nuevo sistema.

---

## 📊 Métricas de éxito
| KPI | Actual | Meta |
|-----|--------|------|
| Usos de `System.getenv()` | 15 | 0 |
| Records de configuración | 2 | 6 (centralizados) |
| Validación automática | Manual | Anotaciones + fail‑fast |
| Logging con MDC | No | Sí (request‑id, user) |
| Fuentes de configuración | Sólo env | Env + sys‑props + YAML |

---

## ⚠️ Riesgos y mitigación
- **Orden de inicialización**: `Db` podría inicializarse antes de cargar `KiwiConfig`. → Lazy init + método `Db.init()` llamado desde `KiwiServer` antes de crear el pool.
- **Breaking changes**: Cambios en constructores. → Mantener versiones deprecated y documentar.
- **Performance**: Carga de configuración en cada acceso. → Cachear `KiwiConfig` como singleton.
- **Complejidad de records**: Demasiados niveles. → Documentar claramente la jerarquía.

---

## 🚀 Próximos pasos
1. Crear la rama `feature/ether-config-integration` (o worktree aislado).
2. Implementar **Fase 1** y **Fase 2**.
3. Ejecutar los tests unitarios y validar que la compilación sigue pasando.
4. Continuar con las fases siguientes de forma incremental.

---

## ❓ Preguntas para ti
1. ¿Prefieres que `KiwiConfig` viva en `kiwi-common` o crear un nuevo módulo `kiwi-config`?
2. ¿Queremos soportar archivos YAML desde el inicio o solo variables de entorno?
3. ¿Es prioritario habilitar MDC ahora o podemos dejarlo para una fase posterior?
4. ¿Algún otro requisito o restricción que deba considerar antes de comenzar?
