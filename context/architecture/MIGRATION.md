# Migration Guide: Ether‑Config Integration

This guide describes how to migrate from the previous configuration system to the new **Ether‑Config** based configuration.

## Overview

Previously, Kiwi backend used a mix of:
- Direct `System.getenv()` calls scattered throughout the code
- Hard‑coded default values
- No centralized validation or type safety

The new system provides:
- **Centralized configuration** via `KiwiConfig` and dedicated record classes
- **Multiple sources** (environment variables, system properties, future YAML files)
- **Automatic validation** with clear error messages
- **Type‑safe access** to all configuration values
- **Singleton pattern** for efficient reuse
- **Testability** through `loadFrom()` and `reset()` methods

## Breaking Changes

### 1. Environment Variable Names

Some variable names have changed for consistency:

| Previous (if any) | New | Notes |
|-------------------|-----|-------|
| `DATABASE_URL` | `DB_URL` | **Required** |
| `DATABASE_USER` | `DB_USER` | |
| `DATABASE_PASSWORD` | `DB_PASSWORD` | |
| `DATABASE_MAX_POOL_SIZE` | `DB_MAX_POOL_SIZE` | Default changed from 10 to 6 |
| `DATABASE_CONNECTION_TIMEOUT_MS` | `DB_CONNECTION_TIMEOUT_MS` | Default changed from 5000 to 30000 |
| `JWT_ISSUER` | `JWT_ISS` | |
| `JWT_AUDIENCE` | `JWT_AUD` | |
| `JWT_EXPIRATION_MINUTES` | `JWT_TTL_SECONDS` | Now in seconds (default 3600) |
| `JWT_APP_EXPIRATION_MINUTES` | `JWT_APP_TTL_SECONDS` | Now in seconds (default 1800) |
| `AUTH_SALT_BYTES` | `AUTH_SALT_BYTES` | No change |
| `AUTH_PBKDF2_ITERATIONS` | `AUTH_PBKDF2_ITERATIONS` | No change |
| `AUTH_PASSWORD_HASH_BYTES` | `KIWI_PASSWORD_HASH_BYTES` | Renamed |
| `LOG_LEVEL` | `LOG_LEVEL` | No change |
| `SERVER_PORT` | `PORT` | |
| `HTTP_MAX_THREADS` | `HTTP_MAX_THREADS` | No change |
| `HTTP_MIN_THREADS` | `HTTP_MIN_THREADS` | No change |
| `HTTP_IDLE_TIMEOUT_MS` | `HTTP_IDLE_TIMEOUT_MS` | No change |
| `HTTP_POOL_NAME` | `HTTP_POOL_NAME` | New |
| `ENVIRONMENT` | `ENVIRONMENT` | No change |
| `ENABLE_USER_PROVISIONING` | `ENABLE_USER_PROVISIONING` | No change |
| `BOOTSTRAP_TOKEN` | `BOOTSTRAP_TOKEN` | No change |

### 2. JWT Secret Handling

**Critical change:** The default JWT secret `"CHANGE_ME_NOW_32+chars_secret"` now triggers **automatic random secret generation** with a warning. This means:

- If you were using the default secret, tokens will become invalid after each restart
- In production, you **must** set `JWT_SECRET` to a persistent 256‑bit (32‑byte) hex or base64 secret

**Migration action:**
```bash
# Generate a secure secret
export JWT_SECRET=$(openssl rand -hex 32)

# Or set your own
export JWT_SECRET=your_256_bit_secret_here
```

### 3. Database Configuration Validation

The database URL (`DB_URL`) is now **required**. If not provided, `DatabaseConfig.from()` throws `IllegalArgumentException`.

Previous code might have allowed missing URL with later failure. Now you must ensure `DB_URL` is set.

### 4. Configuration Access in Code

**Before:**
```java
String dbUrl = System.getenv("DATABASE_URL");
int port = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "8080"));
String jwtSecret = System.getenv("JWT_SECRET");
if (jwtSecret == null) {
    jwtSecret = "default-secret";
}
```

**After:**
```java
KiwiConfig config = KiwiConfig.load();

String dbUrl = config.database().url();
int port = config.server().port();
String jwtSecret = config.jwt().secret(); // Already resolved (random if missing)
```

### 5. Testing Changes

**Before:** Tests might have set environment variables directly or used mocks.

**After:** Use `KiwiConfig.loadFrom()` with a `MapConfigSource`:

```java
@BeforeEach
void setup() {
    Map<String, String> testConfig = new HashMap<>();
    testConfig.put("DB_URL", "jdbc:h2:mem:test");
    testConfig.put("JWT_SECRET", "test-secret");
    testConfig.put("PORT", "8081");
    
    EtherConfig testSource = EtherConfig.of(
        new MapConfigSource("test", testConfig)
    );
    KiwiConfig testInstance = KiwiConfig.loadFrom(testSource);
    
    // Inject testInstance into your component
}
```

## Step‑by‑Step Migration

### 1. Update Environment Variables

Review your deployment scripts, Dockerfiles, Kubernetes configs, and `.env` files. Change variable names according to the table above.

Example `.env` file update:
```diff
- DATABASE_URL=jdbc:postgresql://localhost:5432/kiwi
+ DB_URL=jdbc:postgresql://localhost:5432/kiwi
- DATABASE_USER=kiwi_user
+ DB_USER=kiwi_user
- JWT_ISSUER=kiwi-backend
+ JWT_ISS=kiwi-backend
- JWT_EXPIRATION_MINUTES=60
+ JWT_TTL_SECONDS=3600
```

### 2. Set JWT Secret

If you weren't already setting `JWT_SECRET`, generate one and update your deployment:

```bash
# Generate new secret
openssl rand -hex 32
# Output: abc123... (32-byte hex)

# Add to environment
export JWT_SECRET=abc123...
```

### 3. Update Code References

Search for direct `System.getenv()` calls and replace them with `KiwiConfig` access:

```bash
# Find all direct env accesses
grep -r "System\.getenv" src/main/java/
```

Common patterns to replace:

| Pattern | Replacement |
|---------|-------------|
| `System.getenv("PORT")` | `config.server().port()` |
| `System.getenv("DATABASE_URL")` | `config.database().url()` |
| `System.getenv().getOrDefault("LOG_LEVEL", "INFO")` | `config.logging().level()` |
| `Boolean.parseBoolean(System.getenv("ENABLE_X"))` | `config.server().enableUserProvisioning()` or similar |

### 4. Update Tests

Convert tests that rely on environment variables to use `MapConfigSource`:

```java
// Before
@Test
void testWithEnv() {
    System.setenv("PORT", "9090");
    // test logic
}

// After
@Test
void testWithConfig() {
    Map<String, String> testVars = Map.of("PORT", "9090");
    EtherConfig testSource = EtherConfig.of(
        new MapConfigSource("test", testVars)
    );
    KiwiConfig config = KiwiConfig.loadFrom(testSource);
    // test logic using config
}
```

**Note:** Don't forget to call `KiwiConfig.reset()` between tests if using the singleton.

### 5. Verify Validation

Test edge cases: invalid port numbers, blank secrets, negative timeouts. The new system will throw `IllegalArgumentException` with descriptive messages.

## New Features to Adopt

### 1. Sandbox Detection

Use `config.server().isSandbox()` or `config.auth().isSandbox()` to detect sandbox environments (`work02`, `sandbox`, `dev`).

```java
if (config.server().isSandbox()) {
    // Enable debug endpoints, verbose logging, etc.
}
```

### 2. Database URL Masking

For safe logging, use `config.database().maskedUrl()` instead of the full JDBC URL.

```java
log.info("Connecting to database: {}", config.database().maskedUrl());
// Output: "Connecting to database: localhost:5432/kiwi"
```

### 3. Type‑Safe Configuration

Access configuration with compile‑time safety:

```java
// Before: runtime errors possible
int port = Integer.parseInt(System.getenv("PORT"));

// After: guaranteed to be valid integer
int port = config.server().port();
```

### 4. Multiple Source Support

Future enhancement: YAML configuration files. The architecture is ready to add `YamlFileConfigSource` when needed.

## Rollback Plan

If issues arise, you can temporarily revert by:

1. Keeping the old `System.getenv()` calls alongside the new `KiwiConfig` usage
2. Using feature flags to switch between old and new
3. Gradual migration by module

However, the new system is designed to be backward‑compatible at the environment variable level (with renaming). The safest approach is to update variable names first, then update code.

## Troubleshooting Migration

### "DB_URL environment variable is required"
Set `DB_URL` in your environment. This is now mandatory.

### JWT tokens invalid after upgrade
You're using the default secret. Set `JWT_SECRET` to a persistent value.

### NullPointerException when accessing config
Ensure you call `KiwiConfig.load()` or `KiwiConfig.loadFrom()` before accessing configuration components.

### Configuration values not updating
`KiwiConfig.load()` returns a singleton. Restart the application or call `KiwiConfig.reset()` (in tests).

### Test failures after migration
Update tests to use `MapConfigSource` instead of setting environment variables directly. Remember that environment variables are process‑global and can leak between tests.

## Support

For questions or issues with migration, refer to:
- [CONFIGURATION.md](./CONFIGURATION.md) – Complete configuration reference
- Ether‑Config library documentation
- Existing tests in `kiwi-common/src/test/java/dev/rafex/kiwi/config/`