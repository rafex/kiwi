# AGENTS.md

## Kiwi HTTP Evolution -- ResourceHandler + QuerySpec + Lightweight RSQL

Generated at: 2026-03-02T22:22:52.181731 UTC

------------------------------------------------------------------------

# 1. Objective

Evolve the current Jetty 12 + Java 21 backend architecture to support:

-   Reusable Resource-based HTTP handlers
-   Centralized method dispatch (GET, POST, PUT, DELETE, PATCH, OPTIONS)
-   Lightweight RSQL filtering
-   Standard query parameters (limit, offset, sort, tags, etc.)
-   Safe SQL generation using PreparedStatements
-   Field whitelisting for security
-   Extensible architecture aligned with hexagonal principles

------------------------------------------------------------------------

# 2. Architectural Principles

-   Each HTTP resource owns its basePath()
-   Each resource overrides only required HTTP methods
-   All routing logic is centralized in a reusable ResourceHandler
-   Query parameters + RSQL are merged into a single QuerySpec
-   SQL generation is separated from parsing
-   All fields are validated through explicit mappers (no dynamic SQL
    injection)

------------------------------------------------------------------------

# 3. Core Components

## 3.1 HttpResource Interface

Defines HTTP method hooks.

Methods: - get(HttpExchange) - post(HttpExchange) - put(HttpExchange) -
delete(HttpExchange) - patch(HttpExchange) - options(HttpExchange)

Default behavior returns 405 if not overridden.

------------------------------------------------------------------------

## 3.2 ResourceHandler

Responsibilities: - Match basePath() - Parse path params - Parse query
params - Dispatch to correct HTTP method - Centralize error handling -
Provide common JSON/text responses

Each resource extends this class.

Example: - /book - /object - /location

------------------------------------------------------------------------

# 4. Lightweight RSQL Support

## 4.1 Supported Operators

-   ==
-   !=
-   =in=
-   =out=
-   =like=
-   AND (;)
-   OR (,)
-   Parentheses grouping

Example queries:

q=name=="cosas" q=status=in=(active,pending)
q=(status==active;name=like="%portero%")

------------------------------------------------------------------------

## 4.2 RSQL AST Model

RsqlNode: - And(List`<RsqlNode>`{=html}) - Or(List`<RsqlNode>`{=html}) -
Comp(selector, op, args)

Operators enum: - EQ - NEQ - IN - OUT - LIKE

------------------------------------------------------------------------

## 4.3 RSQL → SQL Translation

Translation must: - Use PreparedStatement placeholders (?) - Use a
FieldMapper whitelist - Never concatenate raw values

Example output:

WHERE (status = ?) AND (tags IN (?,?))

Parameters stored separately.

------------------------------------------------------------------------

# 5. Query Parameters + RSQL Merge

Supported standard params:

-   q (RSQL)
-   limit
-   offset
-   sort=name,-createdAt
-   tags=a,b
-   locationId=uuid
-   enabled=true

Merge rule:

FINAL_FILTER = (RSQL_FILTER) AND (QUERY_PARAMS_FILTERS)

If q is empty → only query param filters apply.

------------------------------------------------------------------------

# 6. QuerySpec Abstraction

QuerySpec contains:

-   RsqlNode filter
-   int limit
-   int offset
-   List`<Sort>`{=html}

Sort: - field - direction (ASC \| DESC)

Defaults: - limit = 20 - offset = 0

Limit must be clamped (1--200 recommended).

------------------------------------------------------------------------

# 7. QuerySpec → SQL Builder

Responsibilities:

-   Convert filter → WHERE clause
-   Convert sort → ORDER BY clause (whitelisted)
-   Append LIMIT ? OFFSET ?
-   Return SQL fragments + parameter list

All sorting fields must be validated through SortMapper.

------------------------------------------------------------------------

# 8. Security Model

Mandatory safeguards:

1.  FieldMapper whitelist
2.  SortMapper whitelist
3.  PreparedStatements only
4.  No raw SQL concatenation
5.  Clamp limit
6.  Validate numeric inputs
7.  Reject unknown selectors

------------------------------------------------------------------------

# 9. Example Endpoint Flow

GET
/object/search?q=status==active&tags=football&limit=10&sort=-createdAt

Flow:

1.  ResourceHandler handles route
2.  QuerySpecBuilder merges filters
3.  RsqlParser parses q
4.  QuerySpecToSql builds SQL
5.  Repository executes PreparedStatement
6.  JSON response returned

------------------------------------------------------------------------

# 10. PostgreSQL Considerations

If tags stored as:

-   text\[\] → use:
    -   && (overlap)
    -   @\> (contains)

If JSONB: - @\> - ?\| operator

Adjust RSQL → SQL translator accordingly.

------------------------------------------------------------------------

# 11. Extension Roadmap

Future improvements:

-   Support gt, ge, lt, le operators
-   Null checks (field==null)
-   BETWEEN operator
-   Full text search integration
-   JSONB nested selectors
-   OpenTelemetry tracing integration
-   Validation layer before QuerySpecBuilder
-   Typed selector metadata registry

------------------------------------------------------------------------

# 12. Alignment with Hexagonal Architecture

Domain Layer: - Pure QuerySpec abstraction

Application Layer: - RSQL parsing - Query merging

Infrastructure Layer: - SQL translation - PostgreSQL-specific operators

Transport Layer: - Jetty ResourceHandler

------------------------------------------------------------------------

# 13. Result

After this evolution, the backend supports:

✔ Reusable HTTP resources\
✔ Lightweight RSQL\
✔ Classic query parameters\
✔ Safe SQL generation\
✔ Strong validation\
✔ Clean separation of concerns\
✔ Production-ready filtering layer

------------------------------------------------------------------------

End of AGENTS.md
