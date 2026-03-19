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
package dev.rafex.kiwi.security;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter de ventana fija en memoria para endpoints de autenticación.
 *
 * <p>Cuenta intentos por clave (tipicamente IP del cliente). Si el número de
 * intentos supera {@code maxAttempts} dentro de {@code windowSeconds} segundos,
 * {@link #tryAcquire(String)} devuelve {@code false}. El contador se resetea
 * cuando la ventana expira o cuando la autenticación es exitosa.
 *
 * <p>Thread-safe: usa {@link ConcurrentHashMap#compute} para actualizar
 * atómicamente cada bucket.
 */
public final class InMemoryRateLimiter {

	private record Bucket(int count, long windowStart) {
	}

	private final int maxAttempts;
	private final long windowSeconds;
	private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

	/**
	 * @param maxAttempts   número máximo de intentos permitidos por ventana
	 * @param windowSeconds duración de la ventana en segundos
	 */
	public InMemoryRateLimiter(final int maxAttempts, final long windowSeconds) {
		if (maxAttempts < 1) {
			throw new IllegalArgumentException("maxAttempts must be >= 1");
		}
		if (windowSeconds < 1) {
			throw new IllegalArgumentException("windowSeconds must be >= 1");
		}
		this.maxAttempts = maxAttempts;
		this.windowSeconds = windowSeconds;
	}

	/**
	 * Registra un intento y devuelve {@code true} si aún está dentro del límite.
	 * Si la ventana expiró, reinicia el contador antes de evaluar.
	 */
	public boolean tryAcquire(final String key) {
		final long now = Instant.now().getEpochSecond();
		final Bucket bucket = buckets.compute(key, (k, b) -> {
			if (b == null || now - b.windowStart() >= windowSeconds) {
				return new Bucket(1, now);
			}
			return new Bucket(b.count() + 1, b.windowStart());
		});
		return bucket.count() <= maxAttempts;
	}

	/**
	 * Elimina el contador para {@code key}. Llamar tras autenticación exitosa
	 * para no penalizar a usuarios legítimos que tuvieron intentos previos fallidos.
	 */
	public void reset(final String key) {
		buckets.remove(key);
	}

	/**
	 * Segundos que quedan hasta que expire la ventana actual para {@code key}.
	 * Útil para poblar el header {@code Retry-After}.
	 */
	public long retryAfterSeconds(final String key) {
		final long now = Instant.now().getEpochSecond();
		final Bucket b = buckets.get(key);
		if (b == null) {
			return 0L;
		}
		return Math.max(0L, windowSeconds - (now - b.windowStart()));
	}
}
