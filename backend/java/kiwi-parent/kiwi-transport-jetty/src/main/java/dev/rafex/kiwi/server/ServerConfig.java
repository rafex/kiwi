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

import java.util.Set;

public record ServerConfig(
		int port,
		int maxThreads,
		int minThreads,
		int idleTimeoutMs,
		String threadPoolName,
		String jwtIssuer,
		String jwtAudience,
		String jwtSecret,
		String environment,
		boolean enableUserProvisioning) {

	public static ServerConfig fromEnv() {
		final var env = System.getenv();
		final var cpus = Runtime.getRuntime().availableProcessors();
		return new ServerConfig(
				parseInt(env.get("PORT"), 8080),
				parseInt(env.get("HTTP_MAX_THREADS"), Math.max(cpus * 2, 16)),
				parseInt(env.get("HTTP_MIN_THREADS"), 4),
				parseInt(env.get("HTTP_IDLE_TIMEOUT_MS"), 30_000),
				env.getOrDefault("HTTP_POOL_NAME", "kiwi-http"),
				env.getOrDefault("JWT_ISS", "dev.rafex.kiwi"),
				env.getOrDefault("JWT_AUD", "kiwi-backend"),
				env.getOrDefault("JWT_SECRET", "CHANGE_ME_NOW_32+chars_secret"),
				env.getOrDefault("ENVIRONMENT", "unknown"),
				"true".equalsIgnoreCase(env.getOrDefault("ENABLE_USER_PROVISIONING", "false")));
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
