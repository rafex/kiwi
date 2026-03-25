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
import java.util.logging.Level;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LoggingConfig}.
 */
@DisplayName("LoggingConfig Tests")
class LoggingConfigTest {

	private static EtherConfig createConfig(Map<String, String> map) {
		return EtherConfig.of(new MapConfigSource("test", map));
	}

	@Test
	@DisplayName("Should load configuration with custom level")
	void shouldLoadConfigurationWithCustomLevel() {
		// Given
		Map<String, String> configMap = new HashMap<>();
		configMap.put("LOG_LEVEL", "DEBUG");
		EtherConfig configSource = createConfig(configMap);

		// When
		LoggingConfig config = LoggingConfig.from(configSource);

		// Then
		assertEquals("DEBUG", config.level());
	}

	@Test
	@DisplayName("Should use default level when not provided")
	void shouldUseDefaultLevelWhenNotProvided() {
		// Given
		Map<String, String> configMap = new HashMap<>();
		EtherConfig configSource = createConfig(configMap);

		// When
		LoggingConfig config = LoggingConfig.from(configSource);

		// Then
		assertEquals("INFO", config.level());
	}

	@Test
	@DisplayName("Should throw exception when level is blank")
	void shouldThrowExceptionWhenLevelIsBlank() {
		// Given
		Map<String, String> configMap = new HashMap<>();
		configMap.put("LOG_LEVEL", "   ");
		EtherConfig configSource = createConfig(configMap);

		// When & Then
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> LoggingConfig.from(configSource));
		assertEquals("level cannot be blank", exception.getMessage());
	}

	@Test
	@DisplayName("Should convert level string to JUL Level")
	void shouldConvertLevelStringToJuliLevel() {
		// Given
		Map<String, String> configMap = new HashMap<>();
		configMap.put("LOG_LEVEL", "WARN");
		EtherConfig configSource = createConfig(configMap);
		LoggingConfig config = LoggingConfig.from(configSource);

		// When
		Level juliLevel = config.toJuliLevel();

		// Then
		assertEquals(Level.WARNING, juliLevel);
	}

	@Test
	@DisplayName("Should convert various level strings to appropriate JUL Levels")
	void shouldConvertVariousLevelStringsToAppropriateJuliLevels() {
		// Map of input level string to expected JUL Level
		Map<String, Level> testCases = Map.ofEntries(Map.entry("SEVERE", Level.SEVERE),
				Map.entry("WARNING", Level.WARNING), Map.entry("WARN", Level.WARNING), Map.entry("INFO", Level.INFO),
				Map.entry("CONFIG", Level.CONFIG), Map.entry("FINE", Level.FINE), Map.entry("FINER", Level.FINER),
				Map.entry("FINEST", Level.FINEST), Map.entry("DEBUG", Level.FINEST), Map.entry("ALL", Level.ALL),
				Map.entry("OFF", Level.OFF), Map.entry("unknown", Level.INFO) // default fallback
		);

		for (Map.Entry<String, Level> entry : testCases.entrySet()) {
			Map<String, String> configMap = new HashMap<>();
			configMap.put("LOG_LEVEL", entry.getKey());
			EtherConfig configSource = createConfig(configMap);
			LoggingConfig config = LoggingConfig.from(configSource);
			assertEquals(entry.getValue(), config.toJuliLevel(), "Failed for level: " + entry.getKey());
		}
	}
}
