# Analysis of Ether Modules for Integration into Kiwi Backend

## Objective
Identify Ether modules that can be reused in the Kiwi Java backend (Jetty 12, Java 21, hexagonal architecture, RSQL → SQL).

---

## 1. Modules of Ether and Their Alignment with Kiwi

| Ether Module | Package | Purpose | Kiwi Area Where It Fits | Integration Priority |
|--------------|---------|---------|--------------------------|----------------------|
| **ether‑http‑core** | `dev.rafex.ether.http.core` | HTTP abstraction (`HttpExchange`, `HttpHandler`, middleware, routing) | Foundation for **ResourceHandler** evolution | **High** |
| **ether‑http‑jetty12** | `dev.rafex.ether.http.jetty12` | Jetty 12 implementation of the HTTP contracts | Direct replacement of current Jetty server (`KiwiServer`) | **High** |
| **ether‑config** | `dev.rafex.ether.config` | Typed configuration binding, secret handling | Replace manual `ServerConfig` / env‑var handling | **High** |
| **ether‑json** | `dev.rafex.ether.json` | JSON codec abstraction (Jackson), schema utilities | Unified JSON (de)serialization across handlers | **High** |
| **ether‑database‑core** | `dev.rafex.ether.database` | Database abstraction (`DatabaseClient`, `RowMapper`, `SqlBuilder`) | Substitute `ObjectQuerySqlBuilder` and JDBC boilerplate | **High** |
| **ether‑jdbc** | `dev.rafex.ether.jdbc` | JDBC implementation of `DatabaseClient`, simple datasource | Complementary DB utilities | **Medium** |
| **ether‑database‑postgres** | `dev.rafex.ether.database.postgres` | PostgreSQL‑specific helpers (array ops, error handling) | Fine‑tune SQL generation for tags, arrays | **Medium** |
| **ether‑http‑client** | `dev.rafex.ether.http.client` | Wrapper around JDK `HttpClient` | Calls to external services / micro‑services | **Medium** |
| **ether‑jwt** | `dev.rafex.ether.jwt` | JWT issuance & verification | Replace current JWT handling | **Medium** |
| **ether‑http‑security** | `dev.rafex.ether.http.security` | CORS, security headers, rate‑limit, IP filtering | Middleware for security hardening | **Medium** |
| **ether‑http‑problem** | `dev.rafex.ether.http.problem` | RFC 7807 Problem Details implementation | Standardised error responses | **Medium** |
| **ether‑observability‑core** | `dev.rafex.ether.observability` | Timing, request‑id, health probes | Add observability without heavy APM | **Medium** |
| **ether‑http‑openapi** | `dev.rafex.ether.http.openapi` | OpenAPI 3.1 builder | Auto‑generate Swagger docs (optional) | **Low** |
| **ether‑websocket‑core / jetty12** | `dev.rafex.ether.websocket` | WebSocket abstraction | Not needed for current HTTP‑only API | **Low** |
| **ether‑webhook** | `dev.rafex.ether.webhook` | Webhook delivery + HMAC verification | Specialized use‑case | **Low** |
| **ether‑glowroot‑jetty12** | `dev.rafex.ether.glowroot.jetty12` | Glowroot APM integration for Jetty | Optional APM integration | **Low** |

---

## 2. Integration Recommendations (Step‑by‑Step)

1. **Add Ether dependencies** to the relevant `pom.xml`s (transport‑jetty, core, infra‑postgres):
   - `ether-http-core`, `ether-http-jetty12`, `ether-config`, `ether-json`, `ether-database-core`
   - Optionally `ether-jwt`, `ether-http-security`, `ether-http-client`.

2. **Replace the HTTP layer**:
   - Refactor `KiwiServer` to use `EtherHttpServer` (Jetty 12 implementation).
   - Migrate existing `ResourceHandler` subclasses to extend `EtherHttpHandler` and use the middleware pipeline (`CorsPolicy`, `SecurityHeadersPolicy`, etc.).

3. **Centralise configuration**:
   - Define a typed config POJO (e.g., `KiwiAppConfig`) and bind it with `ConfigBinder` from `ether-config`.
   - Remove direct `System.getenv` usage.

4. **Unify JSON handling**:
   - Replace direct Jackson usage with `JsonCodec` (`JacksonJsonCodec`).
   - Use `JsonSchemaUtils` for request validation where needed.

5. **Refactor database access**:
   - Introduce `DatabaseClient` and `RowMapper` from `ether-database-core`.
   - Rewrite `ObjectQuerySqlBuilder` using `SqlBuilder` (still respecting field whitelist & prepared‑statement placeholders).
   - Keep PostgreSQL‑specific helpers (`ether-database-postgres`) for array operators (`&&`, `@>`).

6. **JWT authentication**:
   - Switch token creation/verification to `TokenIssuer` / `TokenVerifier` from `ether-jwt`.
   - Plug the verifier into the HTTP middleware chain.

7. **Standardise error responses**:
   - Return `ProblemDetails` objects via `ether-http-problem` instead of ad‑hoc JSON error payloads.

8. **(Optional) Observability & OpenAPI**:
   - Inject `TimingRecorder` & `RequestIdGenerator` for request tracing.
   - Generate an OpenAPI spec with `ether-http-openapi` if documentation is required.

9. **Run the full test suite** and adjust any failing tests (serialization, SQL generation, HTTP responses).

10. **Document the new architecture** in the project README and internal wiki.

---

## 3. Expected Benefits
- **Consistency** – shared contracts (`HttpExchange`, `DatabaseClient`, `Config`) across the codebase.
- **Security** – continued use of prepared statements, plus extra middleware for CORS, headers, rate‑limiting.
- **Maintainability** – clear separation of concerns; each layer can evolve independently.
- **Observability** – built‑in timing and request‑ID without needing a heavyweight APM.
- **Reusability** – the same Ether modules can be used by other services in the organisation.

---

## 4. Next Immediate Actions
1. Create a feature branch (`feature/ether-integration`).
2. Add the listed Ether dependencies to the Maven POMs.
3. Refactor the HTTP server (`KiwiServer`) to the Ether implementation.
4. Run `mvn clean install` and address compilation issues.
5. Iterate through the remaining steps, committing each logical change.

---

*Prepared for the Kiwi team to kick‑off the integration of Ether's lightweight, framework‑free modules.*
