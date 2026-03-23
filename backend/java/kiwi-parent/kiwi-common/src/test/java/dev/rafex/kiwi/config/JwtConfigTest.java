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
import dev.rafex.ether.config.sources.MapConfigSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JwtConfig}.
 */
@DisplayName("JwtConfig Tests")
class JwtConfigTest {

    private static EtherConfig createConfig(Map<String, String> map) {
        return EtherConfig.of(new MapConfigSource("test", map));
    }

    @Test
    @DisplayName("Should load configuration with all values")
    void shouldLoadConfigurationWithAllValues() {
        // Given
        Map<String, String> configMap = new HashMap<>();
        configMap.put("JWT_ISS", "test-issuer");
        configMap.put("JWT_AUD", "test-audience");
        configMap.put("JWT_SECRET", "test-secret-123456789012345678901234567890");
        configMap.put("JWT_TTL_SECONDS", "7200");
        configMap.put("JWT_APP_TTL_SECONDS", "3600");

        EtherConfig configSource = createConfig(configMap);

        // When
        JwtConfig config = JwtConfig.from(configSource);

        // Then
        assertEquals("test-issuer", config.issuer());
        assertEquals("test-audience", config.audience());
        assertEquals("test-secret-123456789012345678901234567890", config.secret());
        assertEquals(7200L, config.ttlSeconds());
        assertEquals(3600L, config.appTtlSeconds());
    }

    @Test
    @DisplayName("Should use default values when not provided")
    void shouldUseDefaultValuesWhenNotProvided() {
        // Given
        Map<String, String> configMap = new HashMap<>();
        configMap.put("JWT_SECRET", "custom-secret-123456789012345678901234567890");
        EtherConfig configSource = createConfig(configMap);

        // When
        JwtConfig config = JwtConfig.from(configSource);

        // Then
        assertEquals("dev.rafex.kiwi", config.issuer()); // default
        assertEquals("kiwi-backend", config.audience()); // default
        assertEquals("custom-secret-123456789012345678901234567890", config.secret());
        assertEquals(3600L, config.ttlSeconds()); // default
        assertEquals(1800L, config.appTtlSeconds()); // default
    }

    @Test
    @DisplayName("Should generate random secret when not provided")
    void shouldGenerateRandomSecretWhenNotProvided() {
        // Given
        Map<String, String> configMap = new HashMap<>();
        // No JWT_SECRET provided
        EtherConfig configSource = createConfig(configMap);

        // When
        JwtConfig config = JwtConfig.from(configSource);

        // Then
        assertEquals("dev.rafex.kiwi", config.issuer());
        assertEquals("kiwi-backend", config.audience());
        assertNotNull(config.secret());
        assertFalse(config.secret().isEmpty());
        // Generated secret should be hex string (64 chars for 32 bytes)
        assertEquals(64, config.secret().length());
        assertTrue(config.secret().matches("[0-9a-fA-F]{64}"));
        assertEquals(3600L, config.ttlSeconds());
        assertEquals(1800L, config.appTtlSeconds());
    }

    @Test
    @DisplayName("Should generate random secret when using default known value")
    void shouldGenerateRandomSecretWhenUsingDefaultKnownValue() {
        // Given
        Map<String, String> configMap = new HashMap<>();
        configMap.put("JWT_SECRET", "CHANGE_ME_NOW_32+chars_secret");
        EtherConfig configSource = createConfig(configMap);

        // When
        JwtConfig config = JwtConfig.from(configSource);

        // Then
        assertNotNull(config.secret());
        assertFalse(config.secret().isEmpty());
        assertEquals(64, config.secret().length());
        assertTrue(config.secret().matches("[0-9a-fA-F]{64}"));
        assertNotEquals("CHANGE_ME_NOW_32+chars_secret", config.secret());
    }


    @Test
    @DisplayName("Should validate ttlSeconds is positive")
    void shouldValidateTtlSecondsIsPositive() {
        // Given
        Map<String, String> configMap = new HashMap<>();
        configMap.put("JWT_SECRET", "test-secret");
        configMap.put("JWT_TTL_SECONDS", "0");
        EtherConfig configSource = createConfig(configMap);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> JwtConfig.from(configSource)
        );
        assertEquals("ttlSeconds must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("Should validate appTtlSeconds is positive")
    void shouldValidateAppTtlSecondsIsPositive() {
        // Given
        Map<String, String> configMap = new HashMap<>();
        configMap.put("JWT_SECRET", "test-secret");
        configMap.put("JWT_APP_TTL_SECONDS", "-1");
        EtherConfig configSource = createConfig(configMap);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> JwtConfig.from(configSource)
        );
        assertEquals("appTtlSeconds must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("Should handle negative ttlSeconds")
    void shouldHandleNegativeTtlSeconds() {
        // Given
        Map<String, String> configMap = new HashMap<>();
        configMap.put("JWT_SECRET", "test-secret");
        configMap.put("JWT_TTL_SECONDS", "-100");
        EtherConfig configSource = createConfig(configMap);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> JwtConfig.from(configSource)
        );
        assertEquals("ttlSeconds must be positive", exception.getMessage());
    }
}