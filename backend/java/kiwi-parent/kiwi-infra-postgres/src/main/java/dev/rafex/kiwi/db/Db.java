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
package dev.rafex.kiwi.db;

import dev.rafex.ether.database.core.DatabaseClient;
import dev.rafex.ether.jdbc.client.JdbcDatabaseClient;
import dev.rafex.kiwi.config.DatabaseConfig;
import dev.rafex.kiwi.config.KiwiConfig;
import dev.rafex.kiwi.logging.Log;

import java.net.URI;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public final class Db {

    private static volatile HikariDataSource DS;
    private static volatile DatabaseClient CLIENT;
    private static volatile DatabaseConfig CONFIG;

    private Db() {
    }

    /**
     * Initialize the database connection pool with the provided configuration.
     * This method must be called before any call to {@link #dataSource()} or {@link #databaseClient()}.
     * If not called, the configuration will be loaded from {@link KiwiConfig#load()} on first access.
     */
    public static synchronized void init(final DatabaseConfig config) {
        if (DS != null) {
            Log.warn(Db.class, "Database already initialized, ignoring re-initialization");
            return;
        }
        CONFIG = config;
        DS = createDataSource(config);
        CLIENT = new JdbcDatabaseClient(DS);
        Log.info(Db.class, "Database initialized with config: " + config.maskedUrl());
    }

    /**
     * Returns the shared DataSource, initializing it if necessary.
     */
    public static DataSource dataSource() {
        ensureInitialized();
        return DS;
    }

    /**
     * Returns the shared DatabaseClient, initializing it if necessary.
     */
    public static DatabaseClient databaseClient() {
        ensureInitialized();
        return CLIENT;
    }

    private static synchronized void ensureInitialized() {
        if (DS == null) {
            // Load configuration from KiwiConfig (which uses environment variables)
            final var config = KiwiConfig.load().database();
            init(config);
        }
    }

    private static HikariDataSource createDataSource(final DatabaseConfig config) {
        final var hikari = new HikariConfig();
        hikari.setJdbcUrl(config.url());
        
        if (config.user() != null && !config.user().isBlank()) {
            hikari.setUsername(config.user());
        }
        if (config.password() != null && !config.password().isBlank()) {
            hikari.setPassword(config.password());
        }
        
        hikari.setMaximumPoolSize(config.maxPoolSize());
        hikari.setMinimumIdle(config.minIdle());
        hikari.setConnectionTimeout(config.connectionTimeoutMs());
        hikari.setIdleTimeout(config.idleTimeoutMs());
        hikari.setMaxLifetime(config.maxLifetimeMs());
        hikari.setValidationTimeout(config.validationTimeoutMs());
        
        hikari.setPoolName("kiwi-pool");
        // PostgreSQL JDBC prepared statement cache
        hikari.addDataSourceProperty("preparedStatementCacheQueries", "256");
        hikari.addDataSourceProperty("preparedStatementCacheSizeMiB", "5");
        
        Log.info(Db.class, "Database connected: " + config.maskedUrl());
        return new HikariDataSource(hikari);
    }

    /**
     * Helper method for masking JDBC URL in logs (kept for backward compatibility).
     */
    public static String maskJdbcUrl(final String dbUrl) {
        if (dbUrl == null || dbUrl.isBlank()) {
            return "<not-set>";
        }
        try {
            final var stripped = dbUrl.startsWith("jdbc:") ? dbUrl.substring(5) : dbUrl;
            final var uri = new URI(stripped);
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