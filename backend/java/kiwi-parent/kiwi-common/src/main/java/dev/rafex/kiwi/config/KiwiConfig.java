/*
 * Copyright 2026 Raúl Eduardo González Argote
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.rafex.kiwi.config;

import dev.rafex.ether.config.EtherConfig;
import dev.rafex.ether.config.sources.EnvironmentConfigSource;
import dev.rafex.ether.config.sources.SystemPropertyConfigSource;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Centralized configuration for Kiwi backend.
 * <p>
 * Loads configuration from multiple sources in order:
 * <ol>
 *   <li>Environment variables (highest priority)</li>
 *   <li>System properties</li>
 *   <li>YAML configuration file (optional, not yet implemented)</li>
 * </ol>
 * <p>
 * Configuration is accessed as a singleton via {@link #load()} or created from
 * a specific {@link EtherConfig} source via {@link #loadFrom(EtherConfig)}.
 * <p>
 * Example usage:
 * <pre>{@code
 * // Load configuration from default sources (environment + system properties)
 * KiwiConfig config = KiwiConfig.load();
 *
 * // Access configuration components
 * DatabaseConfig db = config.database();
 * JwtConfig jwt = config.jwt();
 * AuthConfig auth = config.auth();
 * LoggingConfig logging = config.logging();
 * ServerConfig server = config.server();
 *
 * // Use configuration values
 * int port = server.port();
 * String dbUrl = db.url();
 * String jwtSecret = jwt.secret();
 *
 * // Check if running in sandbox environment
 * if (server.isSandbox()) {
 *     // Enable debug features
 * }
 * }</pre>
 * <p>
 * For testing, use {@link #loadFrom(EtherConfig)} with a {@code MapConfigSource}:
 * <pre>{@code
 * Map<String, String> testValues = Map.of(
 *     "DB_URL", "jdbc:h2:mem:test",
 *     "JWT_SECRET", "test-secret-32-chars"
 * );
 * EtherConfig testSource = EtherConfig.of(
 *     new MapConfigSource("test", testValues)
 * );
 * KiwiConfig testConfig = KiwiConfig.loadFrom(testSource);
 * }</pre>
 *
 * @see DatabaseConfig
 * @see JwtConfig
 * @see AuthConfig
 * @see LoggingConfig
 * @see ServerConfig
 */
public record KiwiConfig(
    DatabaseConfig database,
    JwtConfig jwt,
    AuthConfig auth,
    LoggingConfig logging,
    ServerConfig server
) {
    public KiwiConfig {
        Objects.requireNonNull(database, "database cannot be null");
        Objects.requireNonNull(jwt, "jwt cannot be null");
        Objects.requireNonNull(auth, "auth cannot be null");
        Objects.requireNonNull(logging, "logging cannot be null");
        Objects.requireNonNull(server, "server cannot be null");
    }
    
    private static volatile KiwiConfig INSTANCE;
    
    /**
     * Load configuration from default sources (environment variables and system properties).
     * <p>
     * Returns a singleton instance. Subsequent calls return the same instance unless
     * {@link #reset()} has been called.
     * <p>
     * Example:
     * <pre>{@code
     * KiwiConfig config = KiwiConfig.load();
     * int port = config.server().port();
     * String dbUrl = config.database().url();
     * }</pre>
     *
     * @return the singleton configuration instance
     * @throws IllegalArgumentException if configuration validation fails
     * @see #reset()
     * @see #loadFrom(EtherConfig)
     */
    public static KiwiConfig load() {
        if (INSTANCE == null) {
            synchronized (KiwiConfig.class) {
                if (INSTANCE == null) {
                    INSTANCE = loadFrom(EtherConfig.of(
                        new EnvironmentConfigSource(),
                        new SystemPropertyConfigSource()
                        // YAML support can be added later:
                        // new YamlFileConfigSource(Path.of("config/application.yaml"))
                    ));
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * Load configuration from a specific EtherConfig instance.
     * <p>
     * This method is useful for testing or when you need to load configuration
     * from custom sources (e.g., a {@code MapConfigSource} with test values).
     * <p>
     * Example for testing:
     * <pre>{@code
     * Map<String, String> testConfig = new HashMap<>();
     * testConfig.put("DB_URL", "jdbc:h2:mem:test");
     * testConfig.put("JWT_SECRET", "test-secret-32-chars");
     * testConfig.put("PORT", "8081");
     *
     * EtherConfig testSource = EtherConfig.of(
     *     new MapConfigSource("test", testConfig)
     * );
     * KiwiConfig config = KiwiConfig.loadFrom(testSource);
     * }</pre>
     *
     * @param config the EtherConfig source to load from
     * @return a new KiwiConfig instance
     * @throws IllegalArgumentException if configuration validation fails
     * @see #load()
     * @see #fromEnv()
     */
    public static KiwiConfig loadFrom(final EtherConfig config) {
        final var database = DatabaseConfig.from(config);
        final var jwt = JwtConfig.from(config);
        final var auth = AuthConfig.from(config);
        final var logging = LoggingConfig.from(config);
        final var server = ServerConfig.from(config);
        
        return new KiwiConfig(database, jwt, auth, logging, server);
    }
    
    /**
     * Load configuration from environment variables only.
     * <p>
     * This method is useful when you need to ensure configuration comes
     * exclusively from environment variables (not system properties).
     * <p>
     * Example:
     * <pre>{@code
     * KiwiConfig config = KiwiConfig.fromEnv();
     * // Configuration is loaded only from environment variables
     * }</pre>
     *
     * @return a new KiwiConfig instance loaded from environment variables
     * @throws IllegalArgumentException if configuration validation fails
     * @see #load()
     * @see #loadFrom(EtherConfig)
     */
    public static KiwiConfig fromEnv() {
        return loadFrom(EtherConfig.of(new EnvironmentConfigSource()));
    }
    
    /**
     * Reset the singleton configuration instance.
     * <p>
     * This method clears the cached instance, forcing the next call to
     * {@link #load()} to create a new instance from the current environment.
     * Primarily used in tests to isolate configuration between test cases.
     * <p>
     * Example in test:
     * <pre>{@code
     * @BeforeEach
     * void setUp() {
     *     KiwiConfig.reset(); // Ensure fresh configuration for each test
     * }
     * }</pre>
     *
     * @see #load()
     */
    public static void reset() {
        synchronized (KiwiConfig.class) {
            INSTANCE = null;
        }
    }
}