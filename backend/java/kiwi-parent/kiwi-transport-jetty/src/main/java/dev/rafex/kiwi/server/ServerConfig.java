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

import dev.rafex.ether.config.EtherConfig;
import dev.rafex.ether.config.sources.EnvironmentConfigSource;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Set;

public record ServerConfig(int port, int maxThreads, int minThreads, int idleTimeoutMs, String threadPoolName,
		String jwtIssuer, String jwtAudience, String jwtSecret, String environment, boolean enableUserProvisioning) {

	private static final String KNOWN_DEFAULT_SECRET = "CHANGE_ME_NOW_32+chars_secret";

	public static ServerConfig fromEnv() {
		final var cfg = EtherConfig.of(new EnvironmentConfigSource());
		final var cpus = Runtime.getRuntime().availableProcessors();
		return new ServerConfig(cfg.get("PORT").map(Integer::parseInt).orElse(8080),
				cfg.get("HTTP_MAX_THREADS").map(Integer::parseInt).orElse(Math.max(cpus * 2, 16)),
				cfg.get("HTTP_MIN_THREADS").map(Integer::parseInt).orElse(4),
				cfg.get("HTTP_IDLE_TIMEOUT_MS").map(Integer::parseInt).orElse(30_000),
				cfg.get("HTTP_POOL_NAME").orElse("kiwi-http"), cfg.get("JWT_ISS").orElse("dev.rafex.kiwi"),
				cfg.get("JWT_AUD").orElse("kiwi-backend"), resolveJwtSecret(cfg.get("JWT_SECRET").orElse(null)),
				cfg.get("ENVIRONMENT").orElse("unknown"),
				cfg.get("ENABLE_USER_PROVISIONING").map(Boolean::parseBoolean).orElse(false));
	}

	/**
	 * Si JWT_SECRET no está configurado o usa el valor por defecto conocido, genera
	 * un secreto aleatorio de 256 bits y emite una advertencia. El secreto generado
	 * no persiste entre reinicios — todos los tokens emitidos pierden validez. En
	 * producción debe configurarse JWT_SECRET.
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

}
