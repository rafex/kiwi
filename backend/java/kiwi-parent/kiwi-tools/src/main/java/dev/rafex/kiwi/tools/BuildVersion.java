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
package dev.rafex.kiwi.tools;

import java.io.InputStream;
import java.util.Properties;

public final class BuildVersion {

	private static final String UNKNOWN_VERSION = "unknown";
	private static final String ENV_VERSION = "APP_VERSION";
	private static final String RESOURCE_FILE = "/build-info.properties";
	private static final String VERSION = loadVersion();

	private BuildVersion() {
	}

	public static String current() {
		return VERSION;
	}

	private static String loadVersion() {
		final var fromEnv = normalize(System.getenv(ENV_VERSION));
		try (InputStream input = BuildVersion.class.getResourceAsStream(RESOURCE_FILE)) {
			if (input == null) {
				return fromEnv != null ? fromEnv : UNKNOWN_VERSION;
			}

			final var properties = new Properties();
			properties.load(input);

			final var tag = normalize(properties.getProperty("git.closest.tag.name"));
			final var commit = normalize(properties.getProperty("git.commit.id.abbrev"));
			if (tag != null && commit != null) {
				return tag + "+" + commit;
			}
			if (tag != null) {
				return tag;
			}
			if (commit != null) {
				return commit;
			}

			final var described = normalize(properties.getProperty("git.commit.id.describe-short"));
			if (described != null) {
				return described;
			}

			return fromEnv != null ? fromEnv : UNKNOWN_VERSION;
		} catch (final Exception ignored) {
			return fromEnv != null ? fromEnv : UNKNOWN_VERSION;
		}
	}

	private static String normalize(final String value) {
		if (value == null) {
			return null;
		}
		final var trimmed = value.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		return "null".equalsIgnoreCase(trimmed) ? null : trimmed;
	}
}
