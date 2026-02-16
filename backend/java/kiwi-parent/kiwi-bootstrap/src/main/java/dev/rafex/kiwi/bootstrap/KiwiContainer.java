package dev.rafex.kiwi.bootstrap;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import javax.sql.DataSource;

// infra (postgres impl)
// =====================================================
import dev.rafex.kiwi.repository.ObjectRepository;
import dev.rafex.kiwi.repository.impl.ObjectRepositoryImpl;
import dev.rafex.kiwi.services.ObjectService;
import dev.rafex.kiwi.services.impl.ObjectServiceImpl;

/**
 * Zero-deps container (Java 21): - Centralizes wiring - Supports overrides
 * (tests) - Lazy builds components
 */
public final class KiwiContainer {

    /**
     * Overrides for tests or alternative runtimes. If a supplier is present, it
     * replaces the default.
     */
    public record Overrides(Optional<Supplier<KiwiConfig>> config, Optional<Supplier<DataSource>> dataSource, Optional<Supplier<ObjectRepository>> objectRepository,
            Optional<Supplier<ObjectService>> objectService) {
        public Overrides {
            config = config != null ? config : Optional.empty();
            dataSource = dataSource != null ? dataSource : Optional.empty();
            objectRepository = objectRepository != null ? objectRepository : Optional.empty();
            objectService = objectService != null ? objectService : Optional.empty();
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private Supplier<KiwiConfig> config;
            private Supplier<DataSource> dataSource;
            private Supplier<ObjectRepository> objectRepository;
            private Supplier<ObjectService> objectService;

            public Builder config(final Supplier<KiwiConfig> v) {
                config = v;
                return this;
            }

            public Builder dataSource(final Supplier<DataSource> v) {
                dataSource = v;
                return this;
            }

            public Builder objectRepository(final Supplier<ObjectRepository> v) {
                objectRepository = v;
                return this;
            }

            public Builder objectService(final Supplier<ObjectService> v) {
                objectService = v;
                return this;
            }

            public Overrides build() {
                return new Overrides(Optional.ofNullable(config), Optional.ofNullable(dataSource), Optional.ofNullable(objectRepository), Optional.ofNullable(objectService));
            }
        }
    }

    private final Overrides overrides;

    private final Lazy<KiwiConfig> config;
    private final Lazy<DataSource> dataSource;
    private final Lazy<ObjectRepository> objectRepository;
    private final Lazy<ObjectService> objectService;

    public KiwiContainer() {
        this(Overrides.builder().build());
    }

    public KiwiContainer(final Overrides overrides) {
        this.overrides = Objects.requireNonNull(overrides, "overrides");

        config = new Lazy<>(select(overrides.config(), KiwiConfig::fromEnv));
        dataSource = new Lazy<>(select(overrides.dataSource(), () -> DataSourceFactory.create(config())));
        objectRepository = new Lazy<>(select(overrides.objectRepository(), () -> new ObjectRepositoryImpl(dataSource())));
        objectService = new Lazy<>(select(overrides.objectService(), () -> new ObjectServiceImpl(objectRepository())));
    }

    // ---- Public accessors ----

    public KiwiConfig config() {
        return config.get();
    }

    public DataSource dataSource() {
        return dataSource.get();
    }

    public ObjectRepository objectRepository() {
        return objectRepository.get();
    }

    public ObjectService objectService() {
        return objectService.get();
    }

    /**
     * Warm up critical graph nodes so the first request doesn't pay init costs.
     * Also helps fail-fast on bad config or DB connectivity.
     */
    public void warmup() {
        config();
        dataSource();
        objectService();
    }

    private static <T> Supplier<T> select(final Optional<Supplier<T>> override, final Supplier<T> def) {
        return override.orElse(def);
    }

    // ======= Minimal placeholders (replace with your real ones) =======
    public static final class KiwiConfig {
        public static KiwiConfig fromEnv() {
            // Validate env vars here (fail fast)
            return new KiwiConfig();
        }
    }

    public static final class DataSourceFactory {
        public static DataSource create(final KiwiConfig cfg) {
            // Build and return your DataSource (e.g., HikariDataSource)
            // Return type is DataSource, but if it's AutoCloseable we can register it in
            // Closer.
            return null; // replace
        }
    }
}