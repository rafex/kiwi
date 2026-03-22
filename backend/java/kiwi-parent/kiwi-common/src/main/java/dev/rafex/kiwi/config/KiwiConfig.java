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
 *   <li>Environment variables</li>
 *   <li>System properties</li>
 *   <li>YAML configuration file (optional)</li>
 * </ol>
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
     * Useful for testing.
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
     * Legacy method for compatibility with existing code that expects
     * configuration from environment variables only.
     */
    public static KiwiConfig fromEnv() {
        return loadFrom(EtherConfig.of(new EnvironmentConfigSource()));
    }
    
    /**
     * Reload configuration (primarily for testing).
     */
    public static void reset() {
        synchronized (KiwiConfig.class) {
            INSTANCE = null;
        }
    }
}