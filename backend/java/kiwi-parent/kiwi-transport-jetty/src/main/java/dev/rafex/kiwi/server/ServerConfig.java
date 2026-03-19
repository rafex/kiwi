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
package dev.rafex.kiwi.server;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Set;

public record ServerConfig(int port, int maxThreads, int minThreads, int idleTimeoutMs, String threadPoolName,
		String jwtIssuer, String jwtAudience, String jwtSecret, String environment, boolean enableUserProvisioning) {

	private static final String KNOWN_DEFAULT_SECRET = "CHANGE_ME_NOW_32+chars_secret";

	public static ServerConfig fromEnv() {
		final var env = System.getenv();
		final var cpus = Runtime.getRuntime().availableProcessors();
		return new ServerConfig(parseInt(env.get("PORT"), 8080),
				parseInt(env.get("HTTP_MAX_THREADS"), Math.max(cpus * 2, 16)), parseInt(env.get("HTTP_MIN_THREADS"), 4),
				parseInt(env.get("HTTP_IDLE_TIMEOUT_MS"), 30_000), env.getOrDefault("HTTP_POOL_NAME", "kiwi-http"),
				env.getOrDefault("JWT_ISS", "dev.rafex.kiwi"), env.getOrDefault("JWT_AUD", "kiwi-backend"),
				resolveJwtSecret(env.get("JWT_SECRET")),
				env.getOrDefault("ENVIRONMENT", "unknown"),
				"true".equalsIgnoreCase(env.getOrDefault("ENABLE_USER_PROVISIONING", "false")));
	}

	/**
	 * Si JWT_SECRET no está configurado o usa el valor por defecto conocido,
	 * genera un secreto aleatorio de 256 bits y emite una advertencia.
	 * El secreto generado no persiste entre reinicios — todos los tokens
	 * emitidos pierden validez. En producción debe configurarse JWT_SECRET.
	 */
	private static String resolveJwtSecret(final String envValue) {
		if (envValue == null || envValue.isBlank() || KNOWN_DEFAULT_SECRET.equals(envValue)) {
			final var bytes = new byte[32];
			new SecureRandom().nextBytes(bytes);
			final var generated = HexFormat.of().formatHex(bytes);
			System.err.println("[KIWI] ADVERTENCIA: JWT_SECRET no configurado o usa valor por defecto. "
					+ "Se generó un secreto aleatorio — los tokens perderán validez al reiniciar. "
					+ "Configure JWT_SECRET en variables de entorno para producción.");
			return generated;
		}
		return envValue;
	}

	public boolean isSandbox() {
		return Set.of("work02", "sandbox", "dev").contains(environment.toLowerCase());
	}

	private static int parseInt(final String raw, final int def) {
		if (raw == null || raw.isBlank()) {
			return def;
		}
		try {
			return Integer.parseInt(raw.trim());
		} catch (final NumberFormatException e) {
			return def;
		}
	}

}
