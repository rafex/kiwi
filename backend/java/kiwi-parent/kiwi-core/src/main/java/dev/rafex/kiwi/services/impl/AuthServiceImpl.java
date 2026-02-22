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
import dev.rafex.kiwi.repository.UserRepository;
import dev.rafex.kiwi.security.PasswordHasherPBKDF2;
import dev.rafex.kiwi.services.AuthService;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;

public final class AuthServiceImpl implements AuthService {

	private final UserRepository repository;
	private final PasswordHasherPBKDF2 hasher;

	public AuthServiceImpl(final UserRepository userRepo, final PasswordHasherPBKDF2 hasher) {
		repository = Objects.requireNonNull(userRepo);
		this.hasher = Objects.requireNonNull(hasher);
	}

	@Override
	public AuthResult authenticate(final String username, final char[] password) throws Exception {
		if (username == null || username.isBlank() || password == null || password.length == 0) {
			return AuthResult.bad("bad_credentials");
		}

		try {
			final var userOpt = repository.findByUsername(username);
			if (userOpt.isEmpty()) {
				return AuthResult.bad("bad_credentials");
			}

			final var user = userOpt.get();

			// status check
			if (user.status() == null || !"active".equalsIgnoreCase(user.status())) {
				// aquí sí conviene distinguir, para que el frontend sepa que está bloqueado
				return AuthResult.bad("user_disabled");
			}

			// password verify
			final var ok = hasher.verify(password, user.salt(), user.iterations(), user.passwordHash());
			if (!ok) {
				return AuthResult.bad("bad_credentials");
			}

			// roles (activos)
			final var roles = repository.findRoleNamesByUserId(user.userId());

			return AuthResult.ok(user.userId(), user.username(), roles);

		} catch (final SQLException e) {
			// loguea arriba (handler) si quieres; aquí regresamos genérico
			Log.error(getClass(), e, "Error authenticating user {}", username);
			return AuthResult.bad("error");
		} finally {
			// higiene: borra password
			Arrays.fill(password, '\0');
		}
	}
}