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

import java.util.Set;
import java.util.Objects;

/**
 * HTTP server configuration.
 *
 * @param port server port (default 8080)
 * @param maxThreads maximum number of HTTP threads (default max(cpus * 2, 16))
 * @param minThreads minimum number of HTTP threads (default 4)
 * @param idleTimeoutMs thread idle timeout in milliseconds (default 30000)
 * @param threadPoolName name of the thread pool (default "kiwi-http")
 */
public record ServerConfig(
    int port,
    int maxThreads,
    int minThreads,
    int idleTimeoutMs,
    String threadPoolName,
    String environment,
    boolean enableUserProvisioning,
    String bootstrapToken
) {
    public ServerConfig {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        if (maxThreads < 1) {
            throw new IllegalArgumentException("maxThreads must be positive");
        }
        if (minThreads < 1) {
            throw new IllegalArgumentException("minThreads must be positive");
        }
        if (idleTimeoutMs < 0) {
            throw new IllegalArgumentException("idleTimeoutMs cannot be negative");
        }
        Objects.requireNonNull(threadPoolName, "threadPoolName cannot be null");
        Objects.requireNonNull(environment, "environment cannot be null");
        Objects.requireNonNull(bootstrapToken, "bootstrapToken cannot be null");
    }
    
    /**
     * Load server configuration from EtherConfig.
     */
    public static ServerConfig from(final EtherConfig config) {
        final var cpus = Runtime.getRuntime().availableProcessors();
        final var port = config.get("PORT")
            .map(Integer::parseInt)
            .orElse(8080);
        final var maxThreads = config.get("HTTP_MAX_THREADS")
            .map(Integer::parseInt)
            .orElse(Math.max(cpus * 2, 16));
        final var minThreads = config.get("HTTP_MIN_THREADS")
            .map(Integer::parseInt)
            .orElse(4);
        final var idleTimeoutMs = config.get("HTTP_IDLE_TIMEOUT_MS")
            .map(Integer::parseInt)
            .orElse(30_000);
        final var threadPoolName = config.get("HTTP_POOL_NAME")
            .orElse("kiwi-http");
        final var environment = config.get("ENVIRONMENT")
            .orElse("unknown");
        final var enableUserProvisioning = config.get("ENABLE_USER_PROVISIONING")
            .map(Boolean::parseBoolean)
            .orElse(false);
        final var bootstrapToken = config.get("BOOTSTRAP_TOKEN")
            .orElse("");
        
        return new ServerConfig(port, maxThreads, minThreads, idleTimeoutMs, threadPoolName, environment, enableUserProvisioning, bootstrapToken);
    }
    
    public boolean isSandbox() {
        return Set.of("work02", "sandbox", "dev").contains(environment.toLowerCase());
    }
}
