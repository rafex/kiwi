# Code Conventions

## Language & Runtime
- **Java 25** (with preview features enabled)
- **UTF-8** encoding for all source files
- **Modular structure** with Java modules (JPMS)

## Naming
- **Classes**: PascalCase (e.g., `ObjectServiceImpl`, `QuerySpecBuilder`)
- **Interfaces**: PascalCase (e.g., `ObjectService`, `Repository`)
- **Methods/Functions**: camelCase (e.g., `createObject`, `searchWithPagination`)
- **Variables**: camelCase (e.g., `objectId`, `userRepository`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_PAGE_SIZE`, `DEFAULT_TIMEOUT`)
- **Packages**: lowercase with reverse domain (e.g., `dev.rafex.kiwi`)
- **Database tables**: snake_case (e.g., `kiwi_object`, `user_role`)

## Code Style
- **Braces**: K&R style with opening brace on same line
- **Indentation**: 4 spaces (no tabs)
- **Line length**: 120 characters maximum
- **Final variables**: Use `final` for method parameters and local variables when possible
- **Records**: Use Java records for immutable data transfer objects
- **Streams**: Prefer streams over loops for collection transformations
- **Optional**: Use `Optional` for return values that may be absent
- **Exceptions**: Use checked exceptions for recoverable errors, unchecked for programming errors

## Testing
- **Test class naming**: `*Test` for unit tests, `*IT` for integration tests
- **Test method naming**: `should_*_when_*` pattern (e.g., `should_return_user_when_valid_credentials`)
- **Assertions**: Use AssertJ fluent assertions
- **Testcontainers**: Extend `BasePostgresTest` for database tests
- **Test data**: Use builders or factory methods, not raw constructors

## Libraries and Frameworks
| Purpose | Library | Version |
|---------|---------|---------|
| HTTP Server | Jetty | 12.x |
| JSON Processing | Jackson | 2.21.2 |
| Database | PostgreSQL JDBC | 42.x |
| Connection Pool | HikariCP | 5.x |
| Configuration | Ether-Config | 8.1.0 |
| Logging | SLF4J + java.util.logging | 2.x |
| Testing | JUnit 5 | 5.x |
| Container Testing | Testcontainers | 1.19.7 |
| Code Quality | Spotless + Checkstyle | Latest |
| Security Scanning | OWASP Dependency Check | Latest |
| Build Tool | Maven Wrapper | Latest |

## Dependency Injection
- **No framework DI**: Use manual constructor injection
- **Single responsibility**: Each class should have one reason to change
- **Dependency inversion**: Depend on abstractions (interfaces), not concretions
- **Immutability**: Services and repositories should be immutable after construction

## Error Handling
- **Custom exceptions**: Extend `KiwiError` for domain-specific errors
- **Error codes**: Use structured error codes (e.g., `E-001` for foreign key violation)
- **Logging**: Use SLF4J with appropriate log levels (ERROR for exceptions, DEBUG for detailed flow)
- **Validation**: Validate inputs at service boundaries, fail fast

## Database Access
- **Repository pattern**: One repository per aggregate root
- **PreparedStatements only**: Never concatenate SQL strings
- **Transaction boundaries**: Transactions at service layer, not repository
- **Connection management**: Use try-with-resources for all JDBC resources
- **Batch operations**: Use batch updates for bulk operations

## RSQL Query Building
- **Field whitelisting**: Only allow fields explicitly mapped in `FieldMapper`
- **Sort validation**: Validate sort fields through `SortMapper`
- **Limit clamping**: Enforce minimum/maximum limits (1-200 default)
- **Parameter binding**: All values must go through PreparedStatement parameters
- **Operator support**: `==`, `!=`, `=in=`, `=out=`, `=like=`, `AND`, `OR`, parentheses

## What to avoid
- ❌ **Raw SQL concatenation**: Always use PreparedStatements
- ❌ **Magic strings**: Use constants or enums
- ❌ **Global state**: Prefer dependency injection
- ❌ **Mutable shared state**: Use immutable data structures
- ❌ **Catching generic Exception**: Catch specific exception types
- ❌ **Ignoring exceptions**: Always handle or propagate exceptions
- ❌ **Hardcoded configuration**: Use environment variables or config files
- ❌ **Business logic in repositories**: Keep repositories data-access only
- ❌ **Business logic in HTTP layer**: Keep HTTP layer thin, delegate to services

## Commit Conventions
- **Conventional commits**: `feat:`, `fix:`, `chore:`, `docs:`, `test:`, `refactor:`
- **Emoji prefixes**: Optional but encouraged (✨, 🐛, 📝, ✅, ♻️)
- **Scope**: Use parentheses for module (e.g., `feat(auth): add JWT support`)
- **Body**: Explain what and why, not just what
- **Breaking changes**: Use `!` after type/scope and include `BREAKING CHANGE:` in body