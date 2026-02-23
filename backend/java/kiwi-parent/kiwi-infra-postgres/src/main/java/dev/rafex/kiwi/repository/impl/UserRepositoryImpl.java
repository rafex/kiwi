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
package dev.rafex.kiwi.repository.impl;

import dev.rafex.kiwi.repository.UserRepository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

public class UserRepositoryImpl implements UserRepository {

	private final DataSource ds;

	public UserRepositoryImpl(final DataSource ds) {
		this.ds = ds;
	}

	@Override
	public void createUser(final UUID userId, final String username, final byte[] passwordHash, final byte[] salt,
			final int iterations) throws SQLException {

		final var sql = """
				INSERT INTO users (user_id,username, password_hash, salt, iterations, status, created_at, updated_at)
				VALUES (?,?, ?, ?, ?, 'active', NOW(), NOW())
				""";

		try (var c = ds.getConnection(); var ps = c.prepareStatement(sql)) {

			ps.setObject(1, userId);
			ps.setString(2, username);
			ps.setBytes(3, passwordHash);
			ps.setBytes(4, salt);
			ps.setInt(5, iterations);

			ps.executeUpdate();
		}

	}

	/** Para login: trae hash/salt/iterations y status */
	@Override
	public Optional<UserRow> findByUsername(final String username) throws SQLException {
		final var sql = """
				SELECT user_id, username, password_hash, salt, iterations, status, created_at, updated_at
				FROM users
				WHERE username = ?
				""";

		try (var c = ds.getConnection(); var ps = c.prepareStatement(sql)) {

			ps.setString(1, username);

			try (var rs = ps.executeQuery()) {
				if (!rs.next()) {
					return Optional.empty();
				}

				return Optional.of(new UserRow(rs.getObject("user_id", UUID.class), rs.getString("username"),
						rs.getBytes("password_hash"), rs.getBytes("salt"), rs.getInt("iterations"),
						rs.getString("status"), ResultSets.asInstant(rs, "created_at"),
						ResultSets.asInstant(rs, "updated_at")));
			}
		}
	}

	/** Roles del usuario (para meterlos en JWT o checar permisos) */
	@Override
	public List<String> findRoleNamesByUserId(final UUID userId) throws SQLException {
		final var sql = """
				                SELECT role_name
				                FROM api_find_role_names_by_user_id(?::uuid)
				""";

		try (var c = ds.getConnection(); var ps = c.prepareStatement(sql)) {

			ps.setObject(1, userId);

			try (var rs = ps.executeQuery()) {
				final var out = new ArrayList<String>();
				while (rs.next()) {
					out.add(rs.getString(1));
				}
				return out;
			}
		}
	}

	/** Útil si quieres obtener todo (usuario + roles) en una sola llamada */
	@Override
	public Optional<UserWithRoles> findByUsernameWithRoles(final String username) throws SQLException {
		final var userOpt = findByUsername(username);
		if (userOpt.isEmpty()) {
			return Optional.empty();
		}

		final var user = userOpt.get();
		final var roles = findRoleNamesByUserId(user.userId());
		return Optional.of(new UserWithRoles(user, roles));
	}

	@Override
	public int countUsers() throws SQLException {
		try (var c = ds.getConnection();
				var ps = c.prepareStatement("SELECT count(*) FROM users");
				var rs = ps.executeQuery()) {
			rs.next();
			return rs.getInt(1);
		}
	}

}
