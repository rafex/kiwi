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

import dev.rafex.kiwi.db.Db;
import dev.rafex.kiwi.repository.AppClientRepository;
import dev.rafex.kiwi.repository.LocationRepository;
import dev.rafex.kiwi.repository.ObjectRepository;
import dev.rafex.kiwi.repository.RoleRepository;
import dev.rafex.kiwi.repository.UserRepository;
import dev.rafex.kiwi.repository.impl.AppClientRepositoryImpl;
import dev.rafex.kiwi.repository.impl.LocationRepositoryImpl;
import dev.rafex.kiwi.repository.impl.ObjectRepositoryImpl;
import dev.rafex.kiwi.repository.impl.RoleRepositoryImpl;
import dev.rafex.kiwi.repository.impl.UserRepositoryImpl;
import dev.rafex.kiwi.security.PasswordHasherPBKDF2;
import dev.rafex.kiwi.services.AppClientAuthService;
import dev.rafex.kiwi.services.AuthService;
import dev.rafex.kiwi.services.LocationService;
import dev.rafex.kiwi.services.ObjectService;
import dev.rafex.kiwi.services.UserProvisioningService;
import dev.rafex.kiwi.services.impl.AppClientAuthServiceImpl;
import dev.rafex.kiwi.services.impl.AuthServiceImpl;
import dev.rafex.kiwi.services.impl.LocationServiceImpl;
import dev.rafex.kiwi.services.impl.ObjectServiceImpl;
import dev.rafex.kiwi.services.impl.UserProvisioningServiceImpl;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import javax.sql.DataSource;

/**
 * Zero-deps container (Java 21): - Centralizes wiring - Supports overrides
 * (tests) - Lazy builds components
 */
public final class KiwiContainer {

	/**
	 * Overrides for tests or alternative runtimes. If a supplier is present, it
	 * replaces the default.
	 */
	public record Overrides(Optional<Supplier<KiwiConfig>> config, Optional<Supplier<DataSource>> dataSource,
			Optional<Supplier<PasswordHasherPBKDF2>> passwordHasher,
			Optional<Supplier<ObjectRepository>> objectRepository, Optional<Supplier<ObjectService>> objectService,
			Optional<Supplier<LocationRepository>> locationRepository,
			Optional<Supplier<LocationService>> locationService, Optional<Supplier<UserRepository>> userRepository,
			Optional<Supplier<RoleRepository>> roleRepository,
			Optional<Supplier<AppClientRepository>> appClientRepository, Optional<Supplier<AuthService>> authService,
			Optional<Supplier<AppClientAuthService>> appClientAuthService,
			Optional<Supplier<UserProvisioningService>> userProvisioningService) {
		public Overrides {
			config = config != null ? config : Optional.empty();
			dataSource = dataSource != null ? dataSource : Optional.empty();
			passwordHasher = passwordHasher != null ? passwordHasher : Optional.empty();
			objectRepository = objectRepository != null ? objectRepository : Optional.empty();
			objectService = objectService != null ? objectService : Optional.empty();
			locationRepository = locationRepository != null ? locationRepository : Optional.empty();
			locationService = locationService != null ? locationService : Optional.empty();
			userRepository = userRepository != null ? userRepository : Optional.empty();
			roleRepository = roleRepository != null ? roleRepository : Optional.empty();
			appClientRepository = appClientRepository != null ? appClientRepository : Optional.empty();
			authService = authService != null ? authService : Optional.empty();
			appClientAuthService = appClientAuthService != null ? appClientAuthService : Optional.empty();
			userProvisioningService = userProvisioningService != null ? userProvisioningService : Optional.empty();
		}

		public static Builder builder() {
			return new Builder();
		}

		public static final class Builder {
			private Supplier<KiwiConfig> config;
			private Supplier<DataSource> dataSource;
			private Supplier<PasswordHasherPBKDF2> passwordHasher;
			private Supplier<ObjectRepository> objectRepository;
			private Supplier<ObjectService> objectService;
			private Supplier<LocationRepository> locationRepository;
			private Supplier<LocationService> locationService;
			private Supplier<UserRepository> userRepository;
			private Supplier<RoleRepository> roleRepository;
			private Supplier<AppClientRepository> appClientRepository;
			private Supplier<AuthService> authService;
			private Supplier<AppClientAuthService> appClientAuthService;
			private Supplier<UserProvisioningService> userProvisioningService;

			public Builder config(final Supplier<KiwiConfig> v) {
				config = v;
				return this;
			}

			public Builder passwordHasher(final Supplier<PasswordHasherPBKDF2> v) {
				passwordHasher = v;
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

			public Builder locationRepository(final Supplier<LocationRepository> v) {
				locationRepository = v;
				return this;
			}

			public Builder locationService(final Supplier<LocationService> v) {
				locationService = v;
				return this;
			}

			public Builder userRepository(final Supplier<UserRepository> v) {
				userRepository = v;
				return this;
			}

			public Builder roleRepository(final Supplier<RoleRepository> v) {
				roleRepository = v;
				return this;
			}

			public Builder appClientRepository(final Supplier<AppClientRepository> v) {
				appClientRepository = v;
				return this;
			}

			public Builder authService(final Supplier<AuthService> v) {
				authService = v;
				return this;
			}

			public Builder appClientAuthService(final Supplier<AppClientAuthService> v) {
				appClientAuthService = v;
				return this;
			}

			public Builder userProvisioningService(final Supplier<UserProvisioningService> v) {
				userProvisioningService = v;
				return this;
			}

			public Overrides build() {
				return new Overrides(Optional.ofNullable(config), Optional.ofNullable(dataSource),
						Optional.ofNullable(passwordHasher), Optional.ofNullable(objectRepository),
						Optional.ofNullable(objectService), Optional.ofNullable(locationRepository),
						Optional.ofNullable(locationService), Optional.ofNullable(userRepository),
						Optional.ofNullable(roleRepository), Optional.ofNullable(appClientRepository),
						Optional.ofNullable(authService), Optional.ofNullable(appClientAuthService),
						Optional.ofNullable(userProvisioningService));
			}
		}
	}

	private final Overrides overrides;

	private final Lazy<KiwiConfig> config;
	private final Lazy<DataSource> dataSource;
	private final Lazy<ObjectRepository> objectRepository;
	private final Lazy<ObjectService> objectService;
	private final Lazy<LocationRepository> locationRepository;
	private final Lazy<LocationService> locationService;
	private final Lazy<UserRepository> userRepository;
	private final Lazy<RoleRepository> roleRepository;
	private final Lazy<AppClientRepository> appClientRepository;
	private final Lazy<PasswordHasherPBKDF2> passwordHasher;
	private final Lazy<AuthService> authService;
	private final Lazy<AppClientAuthService> appClientAuthService;
	private final Lazy<UserProvisioningService> userProvisioningService;

	public KiwiContainer() {
		this(Overrides.builder().build());
	}

	public KiwiContainer(final Overrides overrides) {
		this.overrides = Objects.requireNonNull(overrides, "overrides");

		config = new Lazy<>(select(overrides.config(), KiwiConfig::fromEnv));
		dataSource = new Lazy<>(select(overrides.dataSource(), () -> DataSourceFactory.create(config())));
		passwordHasher = new Lazy<>(select(overrides.passwordHasher(), () -> PasswordHasherFactory.create(config())));

		objectRepository = new Lazy<>(
				select(overrides.objectRepository(), () -> new ObjectRepositoryImpl(dataSource())));
		objectService = new Lazy<>(select(overrides.objectService(), () -> new ObjectServiceImpl(objectRepository())));
		locationRepository = new Lazy<>(
				select(overrides.locationRepository(), () -> new LocationRepositoryImpl(dataSource())));
		locationService = new Lazy<>(
				select(overrides.locationService(), () -> new LocationServiceImpl(locationRepository())));
		userRepository = new Lazy<>(select(overrides.userRepository(), () -> new UserRepositoryImpl(dataSource())));
		roleRepository = new Lazy<>(select(overrides.roleRepository(), () -> new RoleRepositoryImpl(dataSource())));
		appClientRepository = new Lazy<>(
				select(overrides.appClientRepository(), () -> new AppClientRepositoryImpl(dataSource())));
		authService = new Lazy<>(
				select(overrides.authService(), () -> new AuthServiceImpl(userRepository(), passwordHasher())));
		appClientAuthService = new Lazy<>(select(overrides.appClientAuthService(),
				() -> new AppClientAuthServiceImpl(appClientRepository(), passwordHasher())));
		userProvisioningService = new Lazy<>(select(overrides.userProvisioningService(),
				() -> new UserProvisioningServiceImpl(userRepository(), roleRepository(), passwordHasher())));
	}

	// ---- Public accessors ----

	public KiwiConfig config() {
		return config.get();
	}

	public DataSource dataSource() {
		return dataSource.get();
	}

	public PasswordHasherPBKDF2 passwordHasher() {
		return passwordHasher.get();
	}

	public ObjectRepository objectRepository() {
		return objectRepository.get();
	}

	public ObjectService objectService() {
		return objectService.get();
	}

	public LocationRepository locationRepository() {
		return locationRepository.get();
	}

	public LocationService locationService() {
		return locationService.get();
	}

	public UserRepository userRepository() {
		return userRepository.get();
	}

	public RoleRepository roleRepository() {
		return roleRepository.get();
	}

	public AppClientRepository appClientRepository() {
		return appClientRepository.get();
	}

	public AuthService authService() {
		return authService.get();
	}

	public AppClientAuthService appClientAuthService() {
		return appClientAuthService.get();
	}

	public UserProvisioningService userProvisioningService() {
		return userProvisioningService.get();
	}

	/**
	 * Warm up critical graph nodes so the first request doesn't pay init costs.
	 * Also helps fail-fast on bad config or DB connectivity.
	 */
	public void warmup() {
		config();
		dataSource();
		passwordHasher();
		objectRepository();
		locationRepository();
		objectService();
		locationService();
		userRepository();
		roleRepository();
		appClientRepository();
		authService();
		appClientAuthService();
		userProvisioningService();
	}

	private static <T> Supplier<T> select(final Optional<Supplier<T>> override, final Supplier<T> def) {
		return override.orElse(def);
	}

	// ======= Minimal placeholders (replace with your real ones) =======
	public static final class KiwiConfig {
		private static final String ENV_HASH_BYTES = "KIWI_PASSWORD_HASH_BYTES";
		private static final int DEFAULT_HASH_BYTES = 32;

		private final int passwordHashBytes;

		private KiwiConfig(final int passwordHashBytes) {
			if (passwordHashBytes < 16) {
				throw new IllegalArgumentException("passwordHashBytes demasiado pequeño");
			}
			this.passwordHashBytes = passwordHashBytes;
		}

		public static KiwiConfig fromEnv() {
			// Validate env vars here (fail fast)
			final var rawHashBytes = System.getenv(ENV_HASH_BYTES);
			final int hashBytes;
			if (rawHashBytes == null || rawHashBytes.isBlank()) {
				hashBytes = DEFAULT_HASH_BYTES;
			} else {
				try {
					hashBytes = Integer.parseInt(rawHashBytes.trim());
				} catch (final NumberFormatException e) {
					throw new IllegalArgumentException("Invalid " + ENV_HASH_BYTES + ": " + rawHashBytes, e);
				}
			}
			return new KiwiConfig(hashBytes);
		}

		public int passwordHashBytes() {
			return passwordHashBytes;
		}
	}

	public static final class DataSourceFactory {
		public static DataSource create(final KiwiConfig cfg) {
			return Db.dataSource();
		}
	}

	public static final class PasswordHasherFactory {
		public static PasswordHasherPBKDF2 create(final KiwiConfig cfg) {
			return new PasswordHasherPBKDF2(cfg.passwordHashBytes());
		}
	}
}
