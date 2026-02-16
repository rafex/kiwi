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