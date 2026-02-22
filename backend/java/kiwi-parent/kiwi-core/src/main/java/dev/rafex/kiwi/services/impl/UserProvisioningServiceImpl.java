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

import dev.rafex.kiwi.repository.RoleRepository;
import dev.rafex.kiwi.repository.UserRepository;
import dev.rafex.kiwi.security.PasswordHasherPBKDF2;
import dev.rafex.kiwi.services.UserProvisioningService;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class UserProvisioningServiceImpl implements UserProvisioningService {

	private final UserRepository userRepo;
	private final RoleRepository roleRepo;
	private final PasswordHasherPBKDF2 hasher;

	private final SecureRandom rng = new SecureRandom();

	// defaults por env
	private final int saltBytes;
	private final int iterations;

	public UserProvisioningServiceImpl(final UserRepository userRepo, final RoleRepository roleRepo,
			final PasswordHasherPBKDF2 hasher) {
		this.userRepo = Objects.requireNonNull(userRepo);
		this.roleRepo = Objects.requireNonNull(roleRepo);
		this.hasher = Objects.requireNonNull(hasher);

		saltBytes = Integer.parseInt(System.getenv().getOrDefault("AUTH_SALT_BYTES", "16"));
		iterations = Integer.parseInt(System.getenv().getOrDefault("AUTH_PBKDF2_ITERATIONS", "120000"));
		if (saltBytes < 16) {
			throw new IllegalArgumentException("AUTH_SALT_BYTES debe ser >= 16");
		}
		if (iterations < 10_000) {
			throw new IllegalArgumentException("AUTH_PBKDF2_ITERATIONS demasiado bajo");
		}
	}

	/**
	 * Crea usuario + roles en una transacción. - username UNIQUE - password se hash
	 * con PBKDF2-HMAC-SHA256 - roles se crean si no existen (ensureRole)
	 */
	@Override
	public CreateUserResult createUser(final String username, final char[] password, final List<String> roles)
			throws SQLException {

		if (username == null || username.isBlank() || password == null || password.length == 0) {
			return CreateUserResult.bad("invalid_input");
		}

		final var userId = UUID.randomUUID();
		final var salt = new byte[saltBytes];
		rng.nextBytes(salt);

		final var hash = hasher.hash(password, salt, iterations);

		try {
			userRepo.createUser(userId, username, hash.hash(), hash.salt(), hash.iterations());

			// roles (opcional)
			if (roles != null) {
				for (final var r : roles) {
					if (r == null || r.isBlank()) {
						continue;
					}
					final var roleId = roleRepo.ensureRole(r, "Auto-created role: " + r);
					roleRepo.assignRoleToUser(userId, roleId);
				}
			}

			return CreateUserResult.ok(userId);

		} catch (final SQLException e) {

			// Si quieres, aquí detectas violación UNIQUE(username)
			// sin depender del vendor, lo dejamos genérico:
			final var msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
			if (msg.contains("duplicate") || msg.contains("unique") || msg.contains("users_username_key")) {
				return CreateUserResult.bad("username_taken");
			}
			return CreateUserResult.bad("db_error");
		} finally {
			// higiene: borra password
			Arrays.fill(password, '\0');
		}
	}

	@Override
	public boolean existsAnyUser() throws SQLException {

		final var countUsers = userRepo.countUsers();

		return countUsers > 0;
	}

}