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
package dev.rafex.kiwi.services.impl;

import dev.rafex.kiwi.logging.Log;
import dev.rafex.kiwi.repository.AppClientRepository;
import dev.rafex.kiwi.security.PasswordHasherPBKDF2;
import dev.rafex.kiwi.services.AppClientAuthService;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class AppClientAuthServiceImpl implements AppClientAuthService {

	private final AppClientRepository repository;
	private final PasswordHasherPBKDF2 hasher;
	private final SecureRandom random;
	private final int saltBytes;
	private final int iterations;

	public AppClientAuthServiceImpl(final AppClientRepository repository, final PasswordHasherPBKDF2 hasher) {
		this(repository, hasher, new SecureRandom(),
				Integer.parseInt(System.getenv().getOrDefault("AUTH_SALT_BYTES", "16")),
				Integer.parseInt(System.getenv().getOrDefault("AUTH_PBKDF2_ITERATIONS", "120000")));
	}

	public AppClientAuthServiceImpl(final AppClientRepository repository, final PasswordHasherPBKDF2 hasher,
			final SecureRandom random, final int saltBytes, final int iterations) {
		this.repository = Objects.requireNonNull(repository);
		this.hasher = Objects.requireNonNull(hasher);
		this.random = Objects.requireNonNull(random);
		if (saltBytes < 16) {
			throw new IllegalArgumentException("AUTH_SALT_BYTES debe ser >= 16");
		}
		if (iterations < 10_000) {
			throw new IllegalArgumentException("AUTH_PBKDF2_ITERATIONS demasiado bajo");
		}
		this.saltBytes = saltBytes;
		this.iterations = iterations;
	}

	@Override
	public AuthResult authenticate(final String clientId, final char[] clientSecret) throws Exception {
		if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.length == 0) {
			return AuthResult.bad("invalid_client");
		}

		try {
			final var appOpt = repository.findByClientId(clientId);
			if (appOpt.isEmpty()) {
				return AuthResult.bad("invalid_client");
			}

			final var app = appOpt.get();
			if (app.status() == null || !"active".equalsIgnoreCase(app.status())) {
				return AuthResult.bad("client_disabled");
			}

			final var ok = hasher.verify(clientSecret, app.salt(), app.iterations(), app.secretHash());
			if (!ok) {
				return AuthResult.bad("invalid_client");
			}

			repository.touchLastUsed(app.appClientId());
			return AuthResult.ok(app.appClientId(), app.clientId(), app.roles());
		} catch (final SQLException e) {
			Log.error(getClass(), e, "Error authenticating client {}", clientId);
			return AuthResult.bad("error");
		} finally {
			Arrays.fill(clientSecret, '\0');
		}
	}

	@Override
	public CreateClientResult createClient(final String clientId, final String name, final char[] clientSecret,
			final List<String> roles) throws Exception {
		if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.length == 0) {
			return CreateClientResult.bad("invalid_input");
		}

		if (clientId.length() > 120) {
			return CreateClientResult.bad("invalid_input");
		}

		final var normalizedClientId = clientId.trim();
		final var normalizedName = name == null || name.isBlank() ? normalizedClientId : name.trim();
		final var normalizedRoles = normalizeRoles(roles);

		final var appClientId = UUID.randomUUID();
		final var salt = new byte[saltBytes];
		random.nextBytes(salt);

		try {
			final var hash = hasher.hash(clientSecret, salt, iterations);
			repository.createClient(appClientId, normalizedClientId, normalizedName, hash.hash(), salt, iterations,
					normalizedRoles);
			return CreateClientResult.ok(appClientId, normalizedClientId, normalizedName, normalizedRoles);
		} catch (final SQLException e) {
			if ("23505".equals(e.getSQLState())) {
				return CreateClientResult.bad("client_id_taken");
			}
			Log.error(getClass(), e, "Error creating app client {}", normalizedClientId);
			return CreateClientResult.bad("error");
		} finally {
			Arrays.fill(clientSecret, '\0');
		}
	}

	private static List<String> normalizeRoles(final List<String> roles) {
		if (roles == null || roles.isEmpty()) {
			return List.of();
		}
		return roles.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).distinct().toList();
	}
}
