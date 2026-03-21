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

import java.util.Set;

public record ServerConfig(int port, int maxThreads, int minThreads, int idleTimeoutMs, String threadPoolName,
		String jwtIssuer, String jwtAudience, String jwtSecret, String environment, boolean enableUserProvisioning) {

	public static ServerConfig fromEnv() {
		final var cfg = EtherConfig.of(new EnvironmentConfigSource());
		final var cpus = Runtime.getRuntime().availableProcessors();
		return new ServerConfig(
				cfg.get("PORT").map(Integer::parseInt).orElse(8080),
				cfg.get("HTTP_MAX_THREADS").map(Integer::parseInt).orElse(Math.max(cpus * 2, 16)),
				cfg.get("HTTP_MIN_THREADS").map(Integer::parseInt).orElse(4),
				cfg.get("HTTP_IDLE_TIMEOUT_MS").map(Integer::parseInt).orElse(30_000),
				cfg.get("HTTP_POOL_NAME").orElse("kiwi-http"),
				cfg.get("JWT_ISS").orElse("dev.rafex.kiwi"),
				cfg.get("JWT_AUD").orElse("kiwi-backend"),
				resolveJwtSecret(cfg.get("JWT_SECRET").orElse(null)),
				cfg.get("ENVIRONMENT").orElse("unknown"),
				cfg.get("ENABLE_USER_PROVISIONING").map(Boolean::parseBoolean).orElse(false));
	}

	public boolean isSandbox() {
		return Set.of("work02", "sandbox", "dev").contains(environment.toLowerCase());
	}

	private static String resolveJwtSecret(final String raw) {
		if (raw == null || raw.isBlank()) {
			return "CHANGE_ME_NOW_32+chars_secret";
		}
		return raw;
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
