# Manejo de consultas y filtrado en el backend

Este documento describe la arquitectura y el flujo de **filtrado de recursos** implementado en el módulo `kiwi-transport-jetty` y sus dependencias.

## 1. Componentes clave

| Componente | Responsabilidad | Ubicación |
|------------|----------------|-----------|
| **`NonBlockingResourceHandler`** (framework *ether*) | Clase base que centraliza el despacho de rutas HTTP (`GET`, `POST`, `PATCH`, …) y la generación de respuestas JSON. Los *handlers* de dominio (p.ej. `ObjectHandler`, `LocationHandler`) extienden esta clase y sobrescriben `basePath()`, `routes()` y los métodos HTTP que necesiten. | `dev.rafex.ether.http.jetty12` (dependencia) |
| **`ObjectHandler`**, **`LocationHandler`**, … | Implementaciones concretas de recursos. Cada handler delega la lógica de negocio a los *services* y utiliza `QuerySpecBuilder` para interpretar los parámetros de consulta. | `backend/java/kiwi-parent/kiwi-transport-jetty/src/main/java/dev/rafex/kiwi/handlers` |
| **`QuerySpecBuilder`** | Construye un objeto `QuerySpec` a partir de los parámetros HTTP (`q`, `tags`, `locationId`, `enabled`, `sort`, `limit`, `offset`). Fusiona el filtro RSQL (`q`) con filtros clásicos y aplica límites y ordenación seguros. | `backend/java/kiwi-parent/kiwi-core/src/main/java/dev/rafex/kiwi/query/QuerySpecBuilder.java` |
| **`QuerySpec`** | Registro inmutable que contiene: `RsqlNode filter`, `int limit`, `int offset` y `List<Sort> sorts`. Es la abstracción de dominio que viaja entre capas. | `backend/java/kiwi-parent/kiwi-ports/src/main/java/dev/rafex/kiwi/query/QuerySpec.java` |
| **`RsqlParser`** | Parser recursivo que transforma una cadena RSQL en un árbol de nodos (`RsqlNode`). Soporta operadores `==`, `!=`, `=in=`, `=out=`, `=like=` y los conectores `;` (AND) y `,` (OR). | `backend/java/kiwi-parent/kiwi-core/src/main/java/dev/rafex/kiwi/query/RsqlParser.java` |
| **`RsqlNode`** | AST sellado con tres tipos: `And`, `Or` y `Comp`. Cada `Comp` contiene `selector`, `operator` y `args`. | `backend/java/kiwi-parent/kiwi-ports/src/main/java/dev/rafex/kiwi/query/RsqlNode.java` |
| **`ObjectQuerySqlBuilder`** | Traduce un `QuerySpec` a SQL seguro usando placeholders (`?`). Aplica *field whitelisting* (`FIELD_MAPPER`) y *sort whitelisting* (`SORT_MAPPER`). Gestiona casos especiales para `tags` (operadores `&&`, `ANY`) y `enabled`. | `backend/java/kiwi-parent/kiwi-infra-postgres/src/main/java/dev/rafex/kiwi/repository/impl/ObjectQuerySqlBuilder.java` |

## 2. Flujo de una petición GET `/objects/search`

1. **Jetty** recibe la petición y la delega a `ObjectHandler` (ruta `/objects/search`).
2. `ObjectHandler.search()` llama a `querySpecBuilder.fromRawParams(...)` pasando los valores de los query‑params.
3. `QuerySpecBuilder`
   - Parsea `q` con `RsqlParser` → árbol `RsqlNode`.
   - Construye filtros clásicos (`tags`, `locationId`, `enabled`).
   - Fusiona ambos filtros con `AND`.
   - Normaliza `limit`, `offset` y `sort` (clamp y validación).
   - Devuelve un `QuerySpec`.
4. `ObjectService.search(spec)` delega al `ObjectRepository.search(spec)`.
5. `ObjectRepositoryImpl` (infra) usa `ObjectQuerySqlBuilder.build(spec)` para generar:
   - `SELECT … FROM objects o LEFT JOIN locations l …`
   - `WHERE` generado a partir del árbol RSQL mediante `toSql()` (placeholders y `SqlParameter`).
   - `ORDER BY` basado en `SORT_MAPPER`.
   - `LIMIT ? OFFSET ?` con los valores ya validados.
6. El `SqlQuery` se ejecuta mediante `JdbcTemplate` (no mostrado aquí) usando `PreparedStatement`.
7. Los resultados se transforman en `SearchItem` y se devuelven como JSON.

## 3. Modelo de seguridad

- **Whitelist de campos** (`FIELD_MAPPER`) y de ordenación (`SORT_MAPPER`).
- **PreparedStatement** exclusivamente: todos los valores se añaden como `SqlParameter` o `PostgresParameters`. No hay concatenación de valores crudos.
- **Clamping** de `limit` (1‑200) y `offset` (0‑100 000). Valores fuera de rango se ajustan automáticamente.
- **Validación de tipos**: `parseIntClamp` lanza `IllegalArgumentException` si el parámetro no es numérico.
- **Rechazo de selectores desconocidos**: `ObjectQuerySqlBuilder` lanza excepción si el selector no está en `FIELD_MAPPER`.

## 4. Extensibilidad

- Para añadir un nuevo selector o campo de ordenación basta con registrar la columna correspondiente en `FIELD_MAPPER` / `SORT_MAPPER` de `ObjectQuerySqlBuilder`.
- Nuevos operadores (gt, lt, entre…) pueden incorporarse ampliando `RsqlOperator` y añadiendo la lógica en `toSqlComp`.
- La arquitectura sigue los principios **hexagonales**: `QuerySpec` pertenece al dominio, el parser y el builder están en la capa de aplicación/infraestructura, y los *handlers* forman la capa de transporte.

## 5. Referencias en la documentación

- Se añadió este documento a la sección **Arquitectura y módulos** mediante un enlace en `arquitectura.md`.
- Los diagramas de flujo de petición (`GET /objects/search`) pueden visualizarse en la sección *Flujo de una petición*.

---

*Este documento se generó automáticamente para reflejar la evolución descrita en `AGENTS.md` y está alineado con los principios de seguridad y separación de responsabilidades del proyecto.*