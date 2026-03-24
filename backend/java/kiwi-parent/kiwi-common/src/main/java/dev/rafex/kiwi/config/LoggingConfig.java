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
import java.util.logging.Level;

/**
 * Logging configuration.
 *
 * @param level
 *            logging level (default INFO)
 */
public record LoggingConfig(String level) {
	public LoggingConfig {
		Objects.requireNonNull(level, "level cannot be null");
		if (level.isBlank()) {
			throw new IllegalArgumentException("level cannot be blank");
		}
	}

	/**
	 * Load logging configuration from EtherConfig.
	 */
	public static LoggingConfig from(final EtherConfig config) {
		final var level = config.get("LOG_LEVEL").orElse("INFO");
		return new LoggingConfig(level);
	}

	/**
	 * Convert level string to java.util.logging.Level.
	 */
	public Level toJuliLevel() {
		return switch (level.toUpperCase()) {
			case "SEVERE" -> Level.SEVERE;
			case "WARNING", "WARN" -> Level.WARNING;
			case "INFO" -> Level.INFO;
			case "CONFIG" -> Level.CONFIG;
			case "FINE" -> Level.FINE;
			case "FINER" -> Level.FINER;
			case "FINEST", "DEBUG" -> Level.FINEST;
			case "ALL" -> Level.ALL;
			case "OFF" -> Level.OFF;
			default -> Level.INFO;
		};
	}
}