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