# Configuration Guide

Kiwi backend uses **Ether-Config** library for centralized configuration management.
Configuration is loaded from multiple sources in order:

1. **Environment variables** (highest priority)
2. **System properties**
3. **YAML configuration file** (optional, not yet implemented)

## Quick Start

```java
// Load configuration from default sources (env + system props)
KiwiConfig config = KiwiConfig.load();

// Access configuration components
DatabaseConfig db = config.database();
JwtConfig jwt = config.jwt();
AuthConfig auth = config.auth();
LoggingConfig logging = config.logging();
ServerConfig server = config.server();

// Use configuration values
int port = server.port();
String dbUrl = db.url();
String jwtSecret = jwt.secret();
```

## Configuration Variables

### Database Configuration (`DatabaseConfig`)

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `DB_URL` | JDBC connection URL | (none) | **Yes** |
| `DB_USER` | Database username | `null` | No |
| `DB_PASSWORD` | Database password | `null` | No |
| `DB_MAX_POOL_SIZE` | Maximum connection pool size | `6` | No |
| `DB_MIN_IDLE` | Minimum idle connections | `2` | No |
| `DB_CONNECTION_TIMEOUT_MS` | Connection timeout in milliseconds | `30000` | No |
| `DB_IDLE_TIMEOUT_MS` | Idle timeout in milliseconds | `600000` | No |
| `DB_MAX_LIFETIME_MS` | Maximum connection lifetime in milliseconds | `1800000` | No |
| `DB_VALIDATION_TIMEOUT_MS` | Validation timeout in milliseconds | `20000` | No |

**Example:**
```bash
export DB_URL=jdbc:postgresql://localhost:5432/kiwi
export DB_USER=kiwi_user
export DB_PASSWORD=secret123
export DB_MAX_POOL_SIZE=10
```

### JWT Configuration (`JwtConfig`)

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `JWT_ISS` | Token issuer | `"dev.rafex.kiwi"` | No |
| `JWT_AUD` | Token audience | `"kiwi-backend"` | No |
| `JWT_SECRET` | JWT signing secret | `"CHANGE_ME_NOW_32+chars_secret"` | No* |
| `JWT_TTL_SECONDS` | Token time-to-live in seconds | `3600` (1 hour) | No |
| `JWT_APP_TTL_SECONDS` | App token TTL in seconds | `1800` (30 minutes) | No |

**Important:** If `JWT_SECRET` is not set or uses the default value, a **random 256-bit secret** will be generated at startup. This causes all issued tokens to become invalid on restart. In production, you **must** configure a persistent secret.

**Example:**
```bash
export JWT_SECRET=$(openssl rand -hex 32)
export JWT_ISS=kiwi-production
export JWT_AUD=kiwi-frontend
export JWT_TTL_SECONDS=7200
```

### Authentication Configuration (`AuthConfig`)

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `AUTH_SALT_BYTES` | Number of bytes for password salt | `16` | No |
| `AUTH_PBKDF2_ITERATIONS` | PBKDF2 iteration count | `120000` | No |
| `KIWI_PASSWORD_HASH_BYTES` | Derived key length in bytes | `32` | No |
| `BOOTSTRAP_TOKEN` | Token for bootstrap operations | `""` (empty) | No |
| `ENABLE_USER_PROVISIONING` | Enable user provisioning API | `false` | No |
| `ENVIRONMENT` | Runtime environment | `"unknown"` | No |

**Sandbox Detection:** Environments `work02`, `sandbox`, and `dev` (case-insensitive) are considered sandboxes (`isSandbox()` returns `true`).

**Example:**
```bash
export AUTH_SALT_BYTES=24
export AUTH_PBKDF2_ITERATIONS=150000
export KIWI_PASSWORD_HASH_BYTES=48
export BOOTSTRAP_TOKEN=super-secret-bootstrap-token
export ENABLE_USER_PROVISIONING=true
export ENVIRONMENT=production
```

### Logging Configuration (`LoggingConfig`)

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `LOG_LEVEL` | Logging level | `"INFO"` | No |

Supported levels: `SEVERE`, `WARNING`/`WARN`, `INFO`, `CONFIG`, `FINE`, `FINER`, `FINEST`/`DEBUG`, `ALL`, `OFF`.

**Example:**
```bash
export LOG_LEVEL=DEBUG
```

### Server Configuration (`ServerConfig`)

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `PORT` | HTTP server port | `8080` | No |
| `HTTP_MAX_THREADS` | Maximum HTTP worker threads | `max(cpus × 2, 16)` | No |
| `HTTP_MIN_THREADS` | Minimum HTTP worker threads | `4` | No |
| `HTTP_IDLE_TIMEOUT_MS` | Thread idle timeout in milliseconds | `30000` | No |
| `HTTP_POOL_NAME` | Thread pool name | `"kiwi-http"` | No |
| `ENVIRONMENT` | Runtime environment | `"unknown"` | No |
| `ENABLE_USER_PROVISIONING` | Enable user provisioning API | `false` | No |
| `BOOTSTRAP_TOKEN` | Token for bootstrap operations | `""` (empty) | No |

**Note:** `ENVIRONMENT` and `BOOTSTRAP_TOKEN` are shared with `AuthConfig` (same source variables).

**Sandbox Detection:** Same as `AuthConfig` (`isSandbox()` method).

**Example:**
```bash
export PORT=9090
export HTTP_MAX_THREADS=32
export HTTP_MIN_THREADS=8
export HTTP_IDLE_TIMEOUT_MS=60000
export HTTP_POOL_NAME=kiwi-api-pool
export ENVIRONMENT=staging
```

## Advanced Usage

### Loading from Specific Sources

```java
// Load from environment variables only
KiwiConfig envOnly = KiwiConfig.fromEnv();

// Load from custom EtherConfig (e.g., for testing)
Map<String, String> testConfig = Map.of(
    "DB_URL", "jdbc:h2:mem:test",
    "JWT_SECRET", "test-secret"
);
EtherConfig customSource = EtherConfig.of(
    new MapConfigSource("test", testConfig)
);
KiwiConfig testConfig = KiwiConfig.loadFrom(customSource);
```

### Singleton Pattern

`KiwiConfig.load()` returns a singleton instance. Use `KiwiConfig.reset()` to clear the singleton (mainly for testing).

```java
// First call loads configuration
KiwiConfig config1 = KiwiConfig.load();

// Subsequent calls return the same instance
KiwiConfig config2 = KiwiConfig.load();
assert config1 == config2;

// Reset for testing
KiwiConfig.reset();
KiwiConfig config3 = KiwiConfig.load();
assert config1 != config3;
```

### Validation

Each configuration class validates its values:

- `DatabaseConfig`: URL cannot be blank, numeric values must be positive or non‑negative.
- `JwtConfig`: Secret cannot be blank, TTL values must be positive.
- `AuthConfig`: Salt bytes, iterations, and derived key bytes must meet minimums.
- `ServerConfig`: Port must be 1–65535, thread counts positive.

Invalid values throw `IllegalArgumentException` at load time.

### Logging Integration

The `LoggingConfig.level()` can be converted to `java.util.logging.Level`:

```java
Level juliLevel = config.logging().toJuliLevel();
```

### Database URL Masking

`DatabaseConfig.maskedUrl()` returns a safe version of the URL for logging (host:port/database):

```java
String safeUrl = config.database().maskedUrl();
// Example: "localhost:5432/kiwi" instead of full JDBC URL with credentials
```

## Environment Setup Examples

### Development

```bash
export DB_URL=jdbc:postgresql://localhost:5432/kiwi_dev
export DB_USER=dev_user
export DB_PASSWORD=dev_pass
export JWT_SECRET=$(openssl rand -hex 32)
export LOG_LEVEL=DEBUG
export ENVIRONMENT=dev
export PORT=8080
```

### Production

```bash
export DB_URL=jdbc:postgresql://db.example.com:5432/kiwi_prod
export DB_USER=kiwi_prod_user
export DB_PASSWORD=secure_password_here
export JWT_SECRET=your_256_bit_hex_secret_here
export LOG_LEVEL=INFO
export ENVIRONMENT=production
export PORT=80
export HTTP_MAX_THREADS=64
export HTTP_MIN_THREADS=16
export HTTP_IDLE_TIMEOUT_MS=30000
```

### Testing

```java
class MyTest {
    @BeforeEach
    void setup() {
        Map<String, String> testVars = new HashMap<>();
        testVars.put("DB_URL", "jdbc:h2:mem:test");
        testVars.put("JWT_SECRET", "test-secret-32-chars-long");
        testVars.put("LOG_LEVEL", "WARN");
        
        EtherConfig testConfig = EtherConfig.of(
            new MapConfigSource("test", testVars)
        );
        KiwiConfig testInstance = KiwiConfig.loadFrom(testConfig);
        // Use testInstance in your test
    }
}
```

## Adding New Configuration

1. Create a new record in `dev.rafex.kiwi.config` package.
2. Implement a static `from(EtherConfig)` method that reads environment variables.
3. Add validation in the compact constructor.
4. Register the new component in `KiwiConfig` record.
5. Update this documentation.

## Troubleshooting

### "DB_URL environment variable is required"
Ensure `DB_URL` is set in the environment or system properties.

### JWT tokens invalid after restart
You are using the auto‑generated secret. Set `JWT_SECRET` to a persistent value.

### Configuration changes not taking effect
The configuration is loaded once (singleton). Restart the application or call `KiwiConfig.reset()` (in tests only).

### Port already in use
Change `PORT` environment variable to an available port (1–65535).

## See Also

- [MIGRATION.md](./MIGRATION.md) – Migration guide from previous configuration system
- [Ether‑Config documentation](https://github.com/rafex/ether-config) – Underlying configuration library