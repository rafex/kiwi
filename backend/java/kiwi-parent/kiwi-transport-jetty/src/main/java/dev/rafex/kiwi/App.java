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
package dev.rafex.kiwi;

import dev.rafex.kiwi.bootstrap.KiwiBootstrap;
import dev.rafex.kiwi.logging.Log;
import dev.rafex.kiwi.server.KiwiServer;

import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

public class App {

	private static final Logger LOG = Logger.getLogger(App.class.getName());

	public static void main(final String[] args) throws Exception {

		// Consistencia global (opcional pero recomendable si quieres resultados
		// deterministas)
		Locale.setDefault(Locale.ROOT);
		// Si quieres UTC siempre:
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

		configureLogging(args);

		LOG.info("Starting Kiwi backend...");

		try (var runtime = KiwiBootstrap.start()) {

			final var container = runtime.container();

			KiwiServer.start(container);

			// Si tu server hace .join(), este hilo queda bloqueado aquí
		}
	}

	private static void configureLogging(final String[] args) {

		// 1) Fuente: env var LOG_LEVEL (default INFO)
		var levelStr = System.getenv().getOrDefault("LOG_LEVEL", "INFO");

		// 2) Override por CLI: --log=DEBUG
		for (final String arg : args) {
			if (arg != null && arg.startsWith("--log=")) {
				levelStr = arg.substring("--log=".length());
				break;
			}
		}

		if (levelStr != null && !levelStr.isBlank() && !Log.isSupportedLevel(levelStr)) {
			System.err.println("[kiwi] Invalid LOG_LEVEL/--log value: '" + levelStr
					+ "'. Allowed: DEBUG, INFO, WARN, ERROR. Defaulting to INFO.");
		}

		final var level = Log.parseLevel(levelStr, Level.INFO);
		Log.configureRootLogger(level);

		LOG.info("Log level set to " + level.getName());
	}
}
