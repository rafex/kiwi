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
 * Unit tests for {@link AuthConfig}.
 */
@DisplayName("AuthConfig Tests")
class AuthConfigTest {

    private static EtherConfig createConfig(Map<String, String> map) {
        return EtherConfig.of(new MapConfigSource("test", map));
    }

    @Test
    @DisplayName("Should load configuration with all values")
    void shouldLoadConfigurationWithAllValues() {
        // Given
        Map<String, String> configMap = new HashMap<>();
        configMap.put("AUTH_SALT_BYTES", "24");
        configMap.put("AUTH_PBKDF2_ITERATIONS", "150000");
        configMap.put("KIWI_PASSWORD_HASH_BYTES", "48");
        configMap.put("BOOTSTRAP_TOKEN", "test-bootstrap-token");
        configMap.put("ENABLE_USER_PROVISIONING", "true");
        configMap.put("ENVIRONMENT", "production");

        EtherConfig configSource = createConfig(configMap);

        // When
        AuthConfig config = AuthConfig.from(configSource);

        // Then
        assertEquals(24, config.saltBytes());
        assertEquals(150000, config.iterations());
        assertEquals(48, config.derivedKeyBytes());
        assertEquals("test-bootstrap-token", config.bootstrapToken());
        assertTrue(config.enableUserProvisioning());
        assertEquals("production", config.environment());
    }

    @Test
    @DisplayName("Should use default values when not provided")
    void shouldUseDefaultValuesWhenNotProvided() {
        // Given
        Map<String, String> configMap = new HashMap<>();
        // No configuration provided
        EtherConfig configSource = createConfig(configMap);

        // When
        AuthConfig config = AuthConfig.from(configSource);

        // Then
        assertEquals(16, config.saltBytes()); // default
        assertEquals(120000, config.iterations()); // default
        assertEquals(32, config.derivedKeyBytes()); // default
        assertEquals("", config.bootstrapToken()); // default (empty string)
        assertFalse(config.enableUserProvisioning()); // default
        assertEquals("unknown", config.environment()); // default
    }

    @Test
    @DisplayName("Should validate saltBytes is positive")
    void shouldValidateSaltBytesIsPositive() {
        // Given
        Map<String, String> configMap = new HashMap<>();
        configMap.put("AUTH_SALT_BYTES", "0");
        EtherConfig configSource = createConfig(configMap);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> AuthConfig.from(configSource)
        );
        assertEquals("saltBytes must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("Should validate iterations is positive")
    void shouldValidateIterationsIsPositive() {
        // Given
        Map<String, String> configMap = new HashMap<>();
        configMap.put("AUTH_PBKDF2_ITERATIONS", "-1");
        EtherConfig configSource = createConfig(configMap);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> AuthConfig.from(configSource)
        );
        assertEquals("iterations must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("Should validate derivedKeyBytes minimum size")
    void shouldValidateDerivedKeyBytesMinimumSize() {
        // Given
        Map<String, String> configMap = new HashMap<>();
        configMap.put("KIWI_PASSWORD_HASH_BYTES", "15");
        EtherConfig configSource = createConfig(configMap);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> AuthConfig.from(configSource)
        );
        assertEquals("derivedKeyBytes too small, minimum 16", exception.getMessage());
    }

    @Test
    @DisplayName("Should accept derivedKeyBytes at minimum size")
    void shouldAcceptDerivedKeyBytesAtMinimumSize() {
        // Given
        Map<String, String> configMap = new HashMap<>();
        configMap.put("KIWI_PASSWORD_HASH_BYTES", "16");
        EtherConfig configSource = createConfig(configMap);

        // When
        AuthConfig config = AuthConfig.from(configSource);

        // Then
        assertEquals(16, config.derivedKeyBytes());
    }

    @Test
    @DisplayName("Should correctly identify sandbox environments")
    void shouldCorrectlyIdentifySandboxEnvironments() {
        // Test work02 environment
        Map<String, String> configMap1 = new HashMap<>();
        configMap1.put("ENVIRONMENT", "work02");
        AuthConfig config1 = AuthConfig.from(createConfig(configMap1));
        assertTrue(config1.isSandbox());

        // Test sandbox environment
        Map<String, String> configMap2 = new HashMap<>();
        configMap2.put("ENVIRONMENT", "sandbox");
        AuthConfig config2 = AuthConfig.from(createConfig(configMap2));
        assertTrue(config2.isSandbox());

        // Test dev environment
        Map<String, String> configMap3 = new HashMap<>();
        configMap3.put("ENVIRONMENT", "dev");
        AuthConfig config3 = AuthConfig.from(createConfig(configMap3));
        assertTrue(config3.isSandbox());

        // Test production environment
        Map<String, String> configMap4 = new HashMap<>();
        configMap4.put("ENVIRONMENT", "production");
        AuthConfig config4 = AuthConfig.from(createConfig(configMap4));
        assertFalse(config4.isSandbox());

        // Test staging environment
        Map<String, String> configMap5 = new HashMap<>();
        configMap5.put("ENVIRONMENT", "staging");
        AuthConfig config5 = AuthConfig.from(createConfig(configMap5));
        assertFalse(config5.isSandbox());

        // Test unknown environment (default)
        Map<String, String> configMap6 = new HashMap<>();
        AuthConfig config6 = AuthConfig.from(createConfig(configMap6));
        assertFalse(config6.isSandbox());
    }

    @Test
    @DisplayName("Should handle case-insensitive environment names")
    void shouldHandleCaseInsensitiveEnvironmentNames() {
        // Test uppercase
        Map<String, String> configMap1 = new HashMap<>();
        configMap1.put("ENVIRONMENT", "WORK02");
        AuthConfig config1 = AuthConfig.from(createConfig(configMap1));
        assertTrue(config1.isSandbox());

        // Test mixed case
        Map<String, String> configMap2 = new HashMap<>();
        configMap2.put("ENVIRONMENT", "SaNdBoX");
        AuthConfig config2 = AuthConfig.from(createConfig(configMap2));
        assertTrue(config2.isSandbox());

        // Test lowercase
        Map<String, String> configMap3 = new HashMap<>();
        configMap3.put("ENVIRONMENT", "dev");
        AuthConfig config3 = AuthConfig.from(createConfig(configMap3));
        assertTrue(config3.isSandbox());
    }

    @Test
    @DisplayName("Should handle empty bootstrap token")
    void shouldHandleEmptyBootstrapToken() {
        // Given
        Map<String, String> configMap = new HashMap<>();
        configMap.put("BOOTSTRAP_TOKEN", "");
        EtherConfig configSource = createConfig(configMap);

        // When
        AuthConfig config = AuthConfig.from(configSource);

        // Then
        assertEquals("", config.bootstrapToken());
    }

    @Test
    @DisplayName("Should parse boolean values correctly")
    void shouldParseBooleanValuesCorrectly() {
        // Test true values
        Map<String, String> configMap1 = new HashMap<>();
        configMap1.put("ENABLE_USER_PROVISIONING", "true");
        AuthConfig config1 = AuthConfig.from(createConfig(configMap1));
        assertTrue(config1.enableUserProvisioning());

        // Test false values
        Map<String, String> configMap2 = new HashMap<>();
        configMap2.put("ENABLE_USER_PROVISIONING", "false");
        AuthConfig config2 = AuthConfig.from(createConfig(configMap2));
        assertFalse(config2.enableUserProvisioning());

        // Test case-insensitive true
        Map<String, String> configMap3 = new HashMap<>();
        configMap3.put("ENABLE_USER_PROVISIONING", "TRUE");
        AuthConfig config3 = AuthConfig.from(createConfig(configMap3));
        assertTrue(config3.enableUserProvisioning());

        // Test case-insensitive false
        Map<String, String> configMap4 = new HashMap<>();
        configMap4.put("ENABLE_USER_PROVISIONING", "FALSE");
        AuthConfig config4 = AuthConfig.from(createConfig(configMap4));
        assertFalse(config4.enableUserProvisioning());
    }
}