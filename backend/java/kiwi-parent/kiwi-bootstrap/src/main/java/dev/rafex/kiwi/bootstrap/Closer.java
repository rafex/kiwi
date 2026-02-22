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
package dev.rafex.kiwi.bootstrap;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Collects resources and closes them in reverse order (LIFO).
 *
 * Java 21 version improvements: - Aggregates close() failures via suppressed
 * exceptions and throws at end. - Uses AtomicBoolean for "close once"
 * semantics.
 */
public final class Closer implements AutoCloseable {

	private final List<AutoCloseable> closeables = new ArrayList<>();
	private final AtomicBoolean closed = new AtomicBoolean(false);

	public <T extends AutoCloseable> T register(final T closeable) {
		if (closeable == null) {
			return null;
		}

		if (closed.get()) {
			// Already closed: best-effort immediate close to avoid leaks.
			try {
				closeable.close();
			} catch (final Exception ignored) {
				// best effort
			}
			return closeable;
		}

		closeables.add(closeable);
		return closeable;
	}

	@Override
	public void close() {
		if (!closed.compareAndSet(false, true)) {
			return;
		}

		RuntimeException aggregated = null;

		for (var i = closeables.size() - 1; i >= 0; i--) {
			try {
				closeables.get(i).close();
			} catch (final Exception e) {
				if (aggregated == null) {
					aggregated = new RuntimeException("Failed closing one or more resources");
				}
				aggregated.addSuppressed(e);
			}
		}

		closeables.clear();

		if (aggregated != null) {
			throw aggregated;
		}
	}
}