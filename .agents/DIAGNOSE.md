# Diagnóstico del Proyecto

_Fecha: 2026-03-25 | Repositorio: kiwi_

---

## 1. Exploración

### Estructura general
El proyecto sigue una arquitectura hexagonal (ports & adapters) con separación clara de capas:
- `src/main/java/kiwi/` - Código fuente organizado por capas
- `src/test/java/` - Tests unitarios
- `src/main/resources/` - Configuración y recursos
- Capas: `domain/`, `application/`, `infrastructure/`, `transport/`
- Módulos: `auth`, `object`, `location`, `tag`, `search`

### Lenguajes y tecnologías
- **Java 25** con features modernas (records, sealed interfaces, pattern matching)
- **Jetty 12** como servidor HTTP embebido
- **PostgreSQL** como base de datos
- **Flyway** para migraciones de base de datos
- **JUnit 5** para testing
- **Jackson** para serialización JSON
- **Maven** como sistema de build

### Sistema de build / dependencias
- **Maven** (`pom.xml` presente)
- Java 25 como versión de compilación
- Dependencias organizadas en `pom.xml`
- Docker-compose para desarrollo local

### Puntos de entrada
- `src/main/java/kiwi/Main.java` - Clase principal que inicia Jetty
- `src/main/java/kiwi/transport/http/HttpServer.java` - Configuración del servidor HTTP
- `src/main/java/kiwi/transport/http/*Handler.java` - Handlers HTTP para endpoints

### Módulos y componentes clave
- **Domain**: Entidades core (`Object`, `Location`, `Tag`, `User`) como records inmutables
- **Application**: Servicios y casos de uso que orquestan la lógica de negocio
- **Infrastructure**: Implementaciones de repositorios (PostgreSQL), conexión JDBC
- **Transport**: Controladores HTTP (Jetty handlers) que exponen la API
- **Relaciones**: Domain → Application → Infrastructure → Transport (dependencia unidireccional)

### Archivos de configuración relevantes
- `.gitignore` - Bien configurado (excluye .env, target/, etc.)
- `Makefile` - Para automatización (carga de .env, tags, hooks)
- `.env.example` - Template para variables de entorno
- `docker-compose.yml` - Para desarrollo con PostgreSQL
- `flyway.conf` - Configuración de migraciones
- `pom.xml` - Configuración Maven con Java 25

### Estado del repositorio
- **Rama actual**: `main`
- **Último commit**: "chore: add .opencode/worktrees/ to gitignore"
- **Archivos sin trackear**: `.agents/` (directorio de agentes), `.opencode/worktrees/` (worktrees)
- **No hay cambios pendientes** para commit
- **Repositorio limpio** sin modificaciones sin commitear

---

## 2. Revisión de calidad

### Problemas estructurales o de diseño
1. **Acoplamiento en transporte HTTP**: Los handlers de Jetty (`*Handler.java`) tienen lógica de parsing de queries y validación que debería estar en la capa de aplicación
2. **Falta de validación centralizada**: No hay un mecanismo unificado para validar inputs (parámetros HTTP, DTOs)
3. **Manejo de errores básico**: Los handlers devuelven códigos HTTP genéricos sin mensajes de error estructurados
4. **Falta de logging estructurado**: Uso de `System.out.println` en lugar de framework de logging

### Deuda técnica identificada
1. **Clases con responsabilidades múltiples**:
   - `ObjectHandler.java` (149 líneas): Maneja parsing de queries, validación, lógica de negocio y respuestas HTTP
   - `SearchHandler.java` (112 líneas): Similar problema
2. **Duplicación de código**:
   - Parsing de query parameters repetido en múltiples handlers
   - Lógica de construcción de respuestas JSON duplicada
   - Validación de UUIDs repetida en varios lugares
3. **Nombres poco claros**:
   - `Object` como nombre de entidad principal es demasiado genérico
   - `ObjectRepository` podría confundirse con repositorio de objetos Java
   - Algunos métodos como `handle()` son demasiado genéricos

### Prácticas del lenguaje no seguidas
1. **Falta de null-safety**: No se usa `@Nullable`/`@NonNull` ni se valida consistentemente
2. **Excepciones no específicas**: Se lanzan `RuntimeException` genéricas en lugar de excepciones de dominio
3. **Recursos no cerrados**: En `PostgresConnection.java`, no hay try-with-resources para `Connection`
4. **Falta de inmutabilidad en algunos DTOs**: No todos los DTOs son records

### Riesgos de seguridad
1. **Falta de sanitización de inputs**: Los parámetros de query (especialmente en búsqueda) no se sanitizan para SQL injection
2. **No hay rate limiting**: Cualquier endpoint puede ser abusado
3. **Falta de autenticación/authorización**: No hay mecanismo de auth en los endpoints
4. **Headers de seguridad HTTP**: No se configuran headers como CSP, HSTS, etc.
5. **Dependencias sin versión fija**: En `pom.xml`, algunas dependencias usan rangos de versión

### Cobertura de tests y documentación
1. **Cobertura de tests baja**:
   - Solo hay 2 archivos de test (`ObjectRepositoryTest.java`, `LocationRepositoryTest.java`)
   - No hay tests para handlers HTTP (capa de transporte)
   - No hay tests de integración
   - No hay tests para servicios de aplicación
2. **Documentación insuficiente**:
   - No hay Javadoc en clases/métodos públicos
   - No hay README.md explicando el proyecto
   - No hay documentación de API (OpenAPI/Swagger)
   - No hay guías de desarrollo o despliegue
3. **Falta de pruebas de carga/rendimiento**: No hay benchmarks o tests de stress

---

## 3. Síntesis ejecutiva

### Resumen del proyecto
Backend HTTP que expone recursos (object, location, tag, search, etc.) mediante Jetty 12, implementando arquitectura hexagonal con Java 25 moderno. Organizado en capas claras (domain, application, infrastructure, transport) con PostgreSQL como persistencia.

### Estado de salud
**🟡 Amarillo** — El proyecto está estructurado y funciona, pero existen varios puntos críticos (acoplamiento en la capa de transporte, falta de validación/seguridad y escasa cobertura de pruebas) que pueden impedir escalar o mantener el código con confianza.

### Top 3 fortalezas
1. **Arquitectura hexagonal bien aplicada** — separación clara de capas y uso de repositorios
2. **Uso de características modernas de Java 25** (records, sealed interfaces, pattern‑matching) que mejoran la inmutabilidad y legibilidad
3. **Infraestructura reproducible** — Docker‑compose, Flyway y Maven facilitan el setup local y la gestión de DB

### Top 3 riesgos o deudas
1. **Acoplamiento de lógica de negocio en los HTTP handlers** — `ObjectHandler` y `SearchHandler` mezclan parsing, validación y lógica de dominio, rompiendo la separación de capas
2. **Falta de validación y sanitización centralizada** — Parámetros de query se procesan manualmente; riesgo de SQL‑injection y errores de tipo
3. **Cobertura de pruebas muy baja y ausencia de documentación** — Sólo 2 tests unitarios de repositorios; nada para handlers, servicios ni API; sin Javadoc ni OpenAPI

### Próximos pasos recomendados
1. **Refactorizar la capa de transporte** — Extraer parsing y validación a la capa `application` (DTOs + validators)
2. **Implementar un framework de validación y sanitización** — Adoptar Hibernate Validator (JSR‑380) o similar para validar DTOs
3. **Añadir logging estructurado** — Reemplazar `System.out.println` por SLF4J + Logback (o Log4j2)
4. **Incrementar la cobertura de pruebas** — Crear tests unitarios para servicios de aplicación y tests de integración
5. **Definir e implementar autenticación/authorización** — Introducir JWT o OAuth2 como primer paso; proteger endpoints críticos
6. **Mejorar la documentación** — Generar Javadoc para clases públicas y añadir OpenAPI/Swagger descriptor
7. **Fijar versiones de dependencias** — Revisar `pom.xml` y reemplazar rangos por versiones exactas
8. **Revisar y renombrar entidades genéricas** — Cambiar `Object` por un nombre más descriptivo (p. ej. `KiwiItem`)

---

## 4. Archivos relevantes

| Archivo | Tipo | Relevancia |
|---------|------|------------|
| `src/main/java/kiwi/Main.java` | entry | Punto de entrada principal que inicia el servidor Jetty |
| `src/main/java/kiwi/transport/http/ObjectHandler.java` | module | Handler HTTP principal que muestra acoplamiento y deuda técnica |
| `src/main/java/kiwi/domain/Object.java` | domain | Entidad principal del dominio (necesita renombre) |
| `src/main/java/kiwi/application/ObjectService.java` | module | Servicio de aplicación que orquesta lógica de negocio |
| `src/main/java/kiwi/infrastructure/db/PostgresConnection.java` | module | Conexión a PostgreSQL (necesita mejor manejo de recursos) |
| `pom.xml` | config | Configuración Maven con dependencias y versión Java 25 |
| `docker-compose.yml` | config | Configuración para desarrollo local con PostgreSQL |
| `Makefile` | config | Automatización de tareas (carga de .env, tags, hooks) |
| `.env.example` | config | Template para variables de entorno (seguridad) |
| `src/test/java/kiwi/infrastructure/db/ObjectRepositoryTest.java` | test | Uno de los pocos tests existentes en el proyecto |
| `src/main/resources/flyway.conf` | config | Configuración de migraciones de base de datos |
| `AGENTS.md` | docs | Documentación de arquitectura y especificaciones del proyecto |