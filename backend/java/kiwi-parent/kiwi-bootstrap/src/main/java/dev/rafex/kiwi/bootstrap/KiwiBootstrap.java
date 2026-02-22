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

import dev.rafex.kiwi.services.ObjectService;

import java.util.Objects;

/**
 * Builds a runtime with container + closer.
 *
 * Java 21 version: - Uses a record for runtime holder. - Adds optional warmup.
 * - Registers closeable resources (DataSource if AutoCloseable).
 */
public final class KiwiBootstrap {

	private KiwiBootstrap() {
	}

	public record KiwiRuntime(KiwiContainer container, Closer closer) implements AutoCloseable {

		public KiwiRuntime {
			Objects.requireNonNull(container, "container");
			Objects.requireNonNull(closer, "closer");
		}

		public ObjectService objectService() {
			return container.objectService();
		}

		@Override
		public void close() {
			closer.close();
		}
	}

	public static KiwiRuntime start() {
		return start(new KiwiContainer(), true);
	}

	public static KiwiRuntime start(final KiwiContainer container, final boolean warmup) {
		Objects.requireNonNull(container, "container");

		final var closer = new Closer();

		if (warmup) {
			container.warmup();
		}

		// Register resources for shutdown
		final var ds = container.dataSource();
		if (ds instanceof final AutoCloseable ac) {
			closer.register(ac);
		}

		final var rt = new KiwiRuntime(container, closer);

		Runtime.getRuntime().addShutdownHook(new Thread(rt::close, "kiwi-shutdown"));

		return rt;
	}
}
