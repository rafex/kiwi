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

import static org.junit.jupiter.api.Assertions.*;

import dev.rafex.ether.config.EtherConfig;
import dev.rafex.ether.config.sources.MapConfigSource;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DatabaseConfig}.
 */
@DisplayName("DatabaseConfig Tests")
class DatabaseConfigTest {

	private static EtherConfig createConfig(Map<String, String> map) {
		return EtherConfig.of(new MapConfigSource("test", map));
	}

	@Test
	@DisplayName("Should load configuration with all values")
	void shouldLoadConfigurationWithAllValues() {
		// Given
		Map<String, String> configMap = new HashMap<>();
		configMap.put("DB_URL", "jdbc:postgresql://localhost:5432/testdb");
		configMap.put("DB_USER", "testuser");
		configMap.put("DB_PASSWORD", "testpass");
		configMap.put("DB_MAX_POOL_SIZE", "10");
		configMap.put("DB_MIN_IDLE", "3");
		configMap.put("DB_CONNECTION_TIMEOUT_MS", "15000");
		configMap.put("DB_IDLE_TIMEOUT_MS", "300000");
		configMap.put("DB_MAX_LIFETIME_MS", "900000");
		configMap.put("DB_VALIDATION_TIMEOUT_MS", "10000");

		EtherConfig configSource = createConfig(configMap);

		// When
		DatabaseConfig config = DatabaseConfig.from(configSource);

		// Then
		assertEquals("jdbc:postgresql://localhost:5432/testdb", config.url());
		assertEquals("testuser", config.user());
		assertEquals("testpass", config.password());
		assertEquals(10, config.maxPoolSize());
		assertEquals(3, config.minIdle());
		assertEquals(15000L, config.connectionTimeoutMs());
		assertEquals(300000L, config.idleTimeoutMs());
		assertEquals(900000L, config.maxLifetimeMs());
		assertEquals(10000L, config.validationTimeoutMs());
	}

	@Test
	@DisplayName("Should use default values when not provided")
	void shouldUseDefaultValuesWhenNotProvided() {
		// Given
		Map<String, String> configMap = new HashMap<>();
		configMap.put("DB_URL", "jdbc:postgresql://localhost:5432/testdb");
		EtherConfig configSource = createConfig(configMap);

		// When
		DatabaseConfig config = DatabaseConfig.from(configSource);

		// Then
		assertEquals("jdbc:postgresql://localhost:5432/testdb", config.url());
		assertNull(config.user());
		assertNull(config.password());
		assertEquals(6, config.maxPoolSize()); // default
		assertEquals(2, config.minIdle()); // default
		assertEquals(30000L, config.connectionTimeoutMs()); // default
		assertEquals(600000L, config.idleTimeoutMs()); // default
		assertEquals(1800000L, config.maxLifetimeMs()); // default
		assertEquals(20000L, config.validationTimeoutMs()); // default
	}

	@Test
	@DisplayName("Should throw exception when DB_URL is missing")
	void shouldThrowExceptionWhenDbUrlIsMissing() {
		// Given
		Map<String, String> configMap = new HashMap<>();
		EtherConfig configSource = createConfig(configMap);

		// When & Then
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> DatabaseConfig.from(configSource));
		assertEquals("DB_URL environment variable is required", exception.getMessage());
	}

	@Test
	@DisplayName("Should throw exception when DB_URL is blank")
	void shouldThrowExceptionWhenDbUrlIsBlank() {
		// Given
		Map<String, String> configMap = new HashMap<>();
		configMap.put("DB_URL", "   ");
		EtherConfig configSource = createConfig(configMap);

		// When & Then
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> DatabaseConfig.from(configSource));
		assertEquals("Database URL cannot be blank", exception.getMessage());
	}
	@Test
	@DisplayName("Should validate maxPoolSize is positive")
	void shouldValidateMaxPoolSizeIsPositive() {
		// Given
		Map<String, String> configMap = new HashMap<>();
		configMap.put("DB_URL", "jdbc:postgresql://localhost:5432/testdb");
		configMap.put("DB_MAX_POOL_SIZE", "0");
		EtherConfig configSource = createConfig(configMap);

		// When & Then
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> DatabaseConfig.from(configSource));
		assertEquals("maxPoolSize must be positive", exception.getMessage());
	}

	@Test
	@DisplayName("Should validate minIdle is not negative")
	void shouldValidateMinIdleIsNotNegative() {
		// Given
		Map<String, String> configMap = new HashMap<>();
		configMap.put("DB_URL", "jdbc:postgresql://localhost:5432/testdb");
		configMap.put("DB_MIN_IDLE", "-1");
		EtherConfig configSource = createConfig(configMap);

		// When & Then
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> DatabaseConfig.from(configSource));
		assertEquals("minIdle cannot be negative", exception.getMessage());
	}

	@Test
	@DisplayName("Should validate timeouts are not negative")
	void shouldValidateTimeoutsAreNotNegative() {
		// Given
		Map<String, String> configMap = new HashMap<>();
		configMap.put("DB_URL", "jdbc:postgresql://localhost:5432/testdb");
		configMap.put("DB_CONNECTION_TIMEOUT_MS", "-1");
		EtherConfig configSource = createConfig(configMap);

		// When & Then
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> DatabaseConfig.from(configSource));
		assertEquals("connectionTimeoutMs cannot be negative", exception.getMessage());
	}

	@Test
	@DisplayName("Should mask URL for logging")
	void shouldMaskUrlForLogging() {
		// Given
		Map<String, String> configMap = new HashMap<>();
		configMap.put("DB_URL", "jdbc:postgresql://localhost:5432/testdb");
		EtherConfig configSource = createConfig(configMap);
		DatabaseConfig config = DatabaseConfig.from(configSource);

		// When
		String maskedUrl = config.maskedUrl();

		// Then
		assertEquals("localhost:5432/testdb", maskedUrl);
	}

	@Test
	@DisplayName("Should handle malformed URL in maskedUrl()")
	void shouldHandleMalformedUrlInMaskedUrl() {
		// Given
		Map<String, String> configMap = new HashMap<>();
		configMap.put("DB_URL", "invalid://url");
		EtherConfig configSource = createConfig(configMap);
		DatabaseConfig config = DatabaseConfig.from(configSource);

		// When
		String maskedUrl = config.maskedUrl();

		// Then
		assertEquals("url", maskedUrl);
	}

	@Test
	@DisplayName("Should handle URL without jdbc prefix in maskedUrl()")
	void shouldHandleUrlWithoutJdbcPrefixInMaskedUrl() {
		// Given
		Map<String, String> configMap = new HashMap<>();
		configMap.put("DB_URL", "postgresql://localhost:5432/testdb");
		EtherConfig configSource = createConfig(configMap);
		DatabaseConfig config = DatabaseConfig.from(configSource);

		// When
		String maskedUrl = config.maskedUrl();

		// Then
		assertEquals("localhost:5432/testdb", maskedUrl);
	}

	@Test
	@DisplayName("Should handle URL without path in maskedUrl()")
	void shouldHandleUrlWithoutPathInMaskedUrl() {
		// Given
		Map<String, String> configMap = new HashMap<>();
		configMap.put("DB_URL", "jdbc:postgresql://localhost:5432");
		EtherConfig configSource = createConfig(configMap);
		DatabaseConfig config = DatabaseConfig.from(configSource);

		// When
		String maskedUrl = config.maskedUrl();

		// Then
		assertEquals("localhost:5432", maskedUrl);
	}
}