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
 * Authentication and authorization configuration.
 *
 * @param saltBytes number of bytes for password salt (default 16)
 * @param iterations PBKDF2 iteration count (default 120000)
 * @param derivedKeyBytes number of bytes for derived key (hash length) (default 32)
 * @param bootstrapToken token for bootstrap operations (optional)
 * @param enableUserProvisioning whether user provisioning is enabled (default false)
 * @param environment runtime environment (e.g., "production", "sandbox") (default "unknown")
 */
public record AuthConfig(
    int saltBytes,
    int iterations,
    int derivedKeyBytes,
    String bootstrapToken,
    boolean enableUserProvisioning,
    String environment
) {
    public AuthConfig {
        if (saltBytes < 1) {
            throw new IllegalArgumentException("saltBytes must be positive");
        }
        if (iterations < 1) {
            throw new IllegalArgumentException("iterations must be positive");
        }
        if (derivedKeyBytes < 16) {
            throw new IllegalArgumentException("derivedKeyBytes too small, minimum 16");
        }
        Objects.requireNonNull(environment, "environment cannot be null");
    }
    
    /**
     * Load authentication configuration from EtherConfig.
     */
    public static AuthConfig from(final EtherConfig config) {
        final var saltBytes = config.get("AUTH_SALT_BYTES")
            .map(Integer::parseInt)
            .orElse(16);
        final var iterations = config.get("AUTH_PBKDF2_ITERATIONS")
            .map(Integer::parseInt)
            .orElse(120000);
        final var derivedKeyBytes = config.get("KIWI_PASSWORD_HASH_BYTES")
            .map(Integer::parseInt)
            .orElse(32);
        final var bootstrapToken = config.get("BOOTSTRAP_TOKEN").orElse("");
        final var enableUserProvisioning = config.get("ENABLE_USER_PROVISIONING")
            .map(Boolean::parseBoolean)
            .orElse(false);
        final var environment = config.get("ENVIRONMENT")
            .orElse("unknown");
        
        return new AuthConfig(saltBytes, iterations, derivedKeyBytes, bootstrapToken, enableUserProvisioning, environment);
    }
    
    /**
     * Returns true if the environment is considered a sandbox (work02, sandbox, dev).
     */
    public boolean isSandbox() {
        final var env = environment.toLowerCase();
        return env.equals("work02") || env.equals("sandbox") || env.equals("dev");
    }
}