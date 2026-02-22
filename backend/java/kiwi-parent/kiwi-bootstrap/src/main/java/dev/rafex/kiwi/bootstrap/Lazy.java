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

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Thread-safe lazy memoized supplier.
 *
 * Java 21 version: - Uses the same proven pattern (volatile + synchronized) for
 * exactly-once init. - Fast path is lock-free after initialization.
 */
public final class Lazy<T> implements Supplier<T> {

	private volatile T value;
	private final Supplier<? extends T> factory;

	public Lazy(final Supplier<? extends T> factory) {
		this.factory = Objects.requireNonNull(factory, "factory");
	}

	@Override
	public T get() {
		var v = value;
		if (v != null) {
			return v;
		}

		synchronized (this) {
			v = value;
			if (v == null) {
				v = Objects.requireNonNull(factory.get(), "factory.get() returned null");
				value = v; // safe publication
			}
			return v;
		}
	}

	public boolean isInitialized() {
		return value != null;
	}
}