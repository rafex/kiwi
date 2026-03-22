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

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Objects;

/**
 * JWT configuration.
 *
 * @param issuer token issuer (default "dev.rafex.kiwi")
 * @param audience token audience (default "kiwi-backend")
 * @param secret JWT secret (required, auto-generated if not provided)
 * @param ttlSeconds token time-to-live in seconds (default 3600)
 * @param appTtlSeconds app token time-to-live in seconds (default 1800)
 */
public record JwtConfig(
    String issuer,
    String audience,
    String secret,
    long ttlSeconds,
    long appTtlSeconds
) {
    private static final String KNOWN_DEFAULT_SECRET = "CHANGE_ME_NOW_32+chars_secret";
    
    public JwtConfig {
        Objects.requireNonNull(issuer, "issuer cannot be null");
        Objects.requireNonNull(audience, "audience cannot be null");
        Objects.requireNonNull(secret, "secret cannot be null");
        if (secret.isBlank()) {
            throw new IllegalArgumentException("secret cannot be blank");
        }
        if (ttlSeconds <= 0) {
            throw new IllegalArgumentException("ttlSeconds must be positive");
        }
        if (appTtlSeconds <= 0) {
            throw new IllegalArgumentException("appTtlSeconds must be positive");
        }
    }
    
    /**
     * Load JWT configuration from EtherConfig.
     */
    public static JwtConfig from(final EtherConfig config) {
        final var issuer = config.get("JWT_ISS")
            .orElse("dev.rafex.kiwi");
        final var audience = config.get("JWT_AUD")
            .orElse("kiwi-backend");
        final var secret = resolveJwtSecret(config.get("JWT_SECRET").orElse(null));
        final var ttlSeconds = config.get("JWT_TTL_SECONDS")
            .map(Long::parseLong)
            .orElse(3600L);
        final var appTtlSeconds = config.get("JWT_APP_TTL_SECONDS")
            .map(Long::parseLong)
            .orElse(1800L);
        
        return new JwtConfig(issuer, audience, secret, ttlSeconds, appTtlSeconds);
    }
    
    /**
     * If JWT_SECRET is not configured or uses the known default value,
     * generate a random 256-bit secret and emit a warning.
     * The generated secret does not persist between restarts — all issued
     * tokens become invalid. In production JWT_SECRET must be configured.
     */
    private static String resolveJwtSecret(final String envValue) {
        if (envValue == null || envValue.isBlank() || KNOWN_DEFAULT_SECRET.equals(envValue)) {
            final var bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
            final var generated = HexFormat.of().formatHex(bytes);
            System.err.println("[KIWI] WARNING: JWT_SECRET not configured or using default value. "
                + "Generated random secret — tokens will become invalid on restart. "
                + "Configure JWT_SECRET in environment variables for production.");
            return generated;
        }
        return envValue;
    }
}