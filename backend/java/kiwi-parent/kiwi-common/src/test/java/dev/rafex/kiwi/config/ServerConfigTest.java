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
 * Unit tests for {@link ServerConfig}.
 */
@DisplayName("ServerConfig Tests")
class ServerConfigTest {

    private static EtherConfig createConfig(Map<String, String> map) {
        return EtherConfig.of(new MapConfigSource("test", map));
    }

    @Test
    @DisplayName("Should load configuration with all values")
    void shouldLoadConfigurationWithAllValues() {
        // Given
        Map<String, String> configMap = new HashMap<>();
        configMap.put("PORT", "9090");
        configMap.put("HTTP_MAX_THREADS", "32");
        configMap.put("HTTP_MIN_THREADS", "8");
        configMap.put("HTTP_IDLE_TIMEOUT_MS", "60000");
        configMap.put("HTTP_POOL_NAME", "custom-pool");
        configMap.put("ENVIRONMENT", "production");
        configMap.put("ENABLE_USER_PROVISIONING", "true");
        configMap.put("BOOTSTRAP_TOKEN", "secret-token");
        EtherConfig configSource = createConfig(configMap);

        // When
        ServerConfig config = ServerConfig.from(configSource);

        // Then
        assertEquals(9090, config.port());
        assertEquals(32, config.maxThreads());
        assertEquals(8, config.minThreads());
        assertEquals(60000, config.idleTimeoutMs());
        assertEquals("custom-pool", config.threadPoolName());
        assertEquals("production", config.environment());
        assertTrue(config.enableUserProvisioning());
        assertEquals("secret-token", config.bootstrapToken());
    }

    @Test
    @DisplayName("Should use default values when not provided")
    void shouldUseDefaultValuesWhenNotProvided() {
        // Given
        Map<String, String> configMap = new HashMap<>();
        EtherConfig configSource = createConfig(configMap);

        // When
        ServerConfig config = ServerConfig.from(configSource);

        // Then
        assertEquals(8080, config.port());
        // maxThreads default depends on available processors
        int expectedMaxThreads = Math.max(Runtime.getRuntime().availableProcessors() * 2, 16);
        assertEquals(expectedMaxThreads, config.maxThreads());
        assertEquals(4, config.minThreads());
        assertEquals(30000, config.idleTimeoutMs());
        assertEquals("kiwi-http", config.threadPoolName());
        assertEquals("unknown", config.environment());
        assertFalse(config.enableUserProvisioning());
        assertEquals("", config.bootstrapToken());
    }

    @Test
    @DisplayName("Should validate port range")
    void shouldValidatePortRange() {
        // Test port too low
        Map<String, String> configMap = new HashMap<>();
        configMap.put("PORT", "0");
        EtherConfig configSource = createConfig(configMap);
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ServerConfig.from(configSource)
        );
        assertEquals("port must be between 1 and 65535", exception.getMessage());

        // Test port too high
        Map<String, String> configMap2 = new HashMap<>();
        configMap2.put("PORT", "65536");
        EtherConfig configSource2 = createConfig(configMap2);
        exception = assertThrows(
            IllegalArgumentException.class,
            () -> ServerConfig.from(configSource2)
        );
        assertEquals("port must be between 1 and 65535", exception.getMessage());
    }

    @Test
    @DisplayName("Should validate maxThreads positive")
    void shouldValidateMaxThreadsPositive() {
        Map<String, String> configMap = new HashMap<>();
        configMap.put("HTTP_MAX_THREADS", "0");
        EtherConfig configSource = createConfig(configMap);
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ServerConfig.from(configSource)
        );
        assertEquals("maxThreads must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("Should validate minThreads positive")
    void shouldValidateMinThreadsPositive() {
        Map<String, String> configMap = new HashMap<>();
        configMap.put("HTTP_MIN_THREADS", "0");
        EtherConfig configSource = createConfig(configMap);
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ServerConfig.from(configSource)
        );
        assertEquals("minThreads must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("Should validate idleTimeoutMs not negative")
    void shouldValidateIdleTimeoutMsNotNegative() {
        Map<String, String> configMap = new HashMap<>();
        configMap.put("HTTP_IDLE_TIMEOUT_MS", "-1");
        EtherConfig configSource = createConfig(configMap);
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ServerConfig.from(configSource)
        );
        assertEquals("idleTimeoutMs cannot be negative", exception.getMessage());
    }

    @Test
    @DisplayName("Should detect sandbox environment")
    void shouldDetectSandboxEnvironment() {
        for (String env : new String[]{"work02", "sandbox", "dev", "WORK02", "SANDBOX", "DEV"}) {
            Map<String, String> configMap = new HashMap<>();
            configMap.put("ENVIRONMENT", env);
            EtherConfig configSource = createConfig(configMap);
            ServerConfig config = ServerConfig.from(configSource);
            assertTrue(config.isSandbox(), "Environment " + env + " should be sandbox");
        }
    }

    @Test
    @DisplayName("Should detect non-sandbox environment")
    void shouldDetectNonSandboxEnvironment() {
        for (String env : new String[]{"production", "staging", "test", "unknown", ""}) {
            Map<String, String> configMap = new HashMap<>();
            configMap.put("ENVIRONMENT", env);
            EtherConfig configSource = createConfig(configMap);
            ServerConfig config = ServerConfig.from(configSource);
            assertFalse(config.isSandbox(), "Environment " + env + " should not be sandbox");
        }
    }

    @Test
    @DisplayName("Should parse boolean enableUserProvisioning")
    void shouldParseBooleanEnableUserProvisioning() {
        Map<String, String> configMap = new HashMap<>();
        configMap.put("ENABLE_USER_PROVISIONING", "true");
        EtherConfig configSource = createConfig(configMap);
        ServerConfig config = ServerConfig.from(configSource);
        assertTrue(config.enableUserProvisioning());

        configMap.put("ENABLE_USER_PROVISIONING", "false");
        configSource = createConfig(configMap);
        config = ServerConfig.from(configSource);
        assertFalse(config.enableUserProvisioning());
    }

    @Test
    @DisplayName("Should handle empty bootstrap token")
    void shouldHandleEmptyBootstrapToken() {
        Map<String, String> configMap = new HashMap<>();
        configMap.put("BOOTSTRAP_TOKEN", "");
        EtherConfig configSource = createConfig(configMap);
        ServerConfig config = ServerConfig.from(configSource);
        assertEquals("", config.bootstrapToken());
    }
}