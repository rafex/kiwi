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

import java.util.Objects;

/**
 * Configuration for database connection pool.
 *
 * @param url JDBC URL (required)
 * @param user database user (optional)
 * @param password database password (optional)
 * @param maxPoolSize maximum pool size (default 6)
 * @param minIdle minimum idle connections (default 2)
 * @param connectionTimeoutMs connection timeout in milliseconds (default 30000)
 * @param idleTimeoutMs idle timeout in milliseconds (default 600000)
 * @param maxLifetimeMs maximum lifetime of a connection in milliseconds (default 1800000)
 * @param validationTimeoutMs validation timeout in milliseconds (default 20000)
 */
public record DatabaseConfig(
    String url,
    String user,
    String password,
    int maxPoolSize,
    int minIdle,
    long connectionTimeoutMs,
    long idleTimeoutMs,
    long maxLifetimeMs,
    long validationTimeoutMs
) {
    public DatabaseConfig {
        Objects.requireNonNull(url, "Database URL cannot be null");
        if (url.isBlank()) {
            throw new IllegalArgumentException("Database URL cannot be blank");
        }
        if (maxPoolSize < 1) {
            throw new IllegalArgumentException("maxPoolSize must be positive");
        }
        if (minIdle < 0) {
            throw new IllegalArgumentException("minIdle cannot be negative");
        }
        if (connectionTimeoutMs < 0) {
            throw new IllegalArgumentException("connectionTimeoutMs cannot be negative");
        }
        if (idleTimeoutMs < 0) {
            throw new IllegalArgumentException("idleTimeoutMs cannot be negative");
        }
        if (maxLifetimeMs < 0) {
            throw new IllegalArgumentException("maxLifetimeMs cannot be negative");
        }
        if (validationTimeoutMs < 0) {
            throw new IllegalArgumentException("validationTimeoutMs cannot be negative");
        }
    }

    /**
     * Load database configuration from EtherConfig.
     */
    public static DatabaseConfig from(final EtherConfig config) {
        final var url = config.get("DB_URL")
            .orElseThrow(() -> new IllegalArgumentException("DB_URL environment variable is required"));
        
        final var user = config.get("DB_USER").orElse(null);
        final var password = config.get("DB_PASSWORD").orElse(null);
        
        final var maxPoolSize = config.get("DB_MAX_POOL_SIZE")
            .map(Integer::parseInt)
            .orElse(6);
        final var minIdle = config.get("DB_MIN_IDLE")
            .map(Integer::parseInt)
            .orElse(2);
        final var connectionTimeoutMs = config.get("DB_CONNECTION_TIMEOUT_MS")
            .map(Long::parseLong)
            .orElse(30000L);
        final var idleTimeoutMs = config.get("DB_IDLE_TIMEOUT_MS")
            .map(Long::parseLong)
            .orElse(600000L);
        final var maxLifetimeMs = config.get("DB_MAX_LIFETIME_MS")
            .map(Long::parseLong)
            .orElse(1800000L);
        final var validationTimeoutMs = config.get("DB_VALIDATION_TIMEOUT_MS")
            .map(Long::parseLong)
            .orElse(20000L);
        
        return new DatabaseConfig(
            url, user, password,
            maxPoolSize, minIdle,
            connectionTimeoutMs, idleTimeoutMs, maxLifetimeMs, validationTimeoutMs
        );
    }
    
    /**
     * Returns a masked version of the URL for logging (host:port/database).
     */
    public String maskedUrl() {
        try {
            final var stripped = url.startsWith("jdbc:") ? url.substring(5) : url;
            final var uri = new java.net.URI(stripped);
            final var host = uri.getHost();
            final var port = uri.getPort();
            final var path = uri.getPath();
            final var sb = new StringBuilder();
            if (host != null) {
                sb.append(host);
            }
            if (port != -1) {
                sb.append(":").append(port);
            }
            if (path != null && !path.isBlank()) {
                sb.append(path);
            }
            final var out = sb.toString();
            return out.isEmpty() ? "<masked>" : out;
        } catch (final Exception e) {
            return "<masked>";
        }
    }
}