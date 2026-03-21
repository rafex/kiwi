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

import dev.rafex.ether.database.core.DatabaseClient;
import dev.rafex.ether.database.core.mapping.ResultSets;
import dev.rafex.ether.database.core.sql.SqlParameter;
import dev.rafex.ether.database.core.sql.SqlQuery;
import dev.rafex.kiwi.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class UserRepositoryImpl implements UserRepository {

	private final DatabaseClient db;

	public UserRepositoryImpl(final DatabaseClient db) {
		this.db = db;
	}

	@Override
	public void createUser(final UUID userId, final String username, final byte[] passwordHash, final byte[] salt,
			final int iterations) {
		final var sql = """
				INSERT INTO users (user_id, username, password_hash, salt, iterations, status, created_at, updated_at)
				VALUES (?, ?, ?, ?, ?, 'active', NOW(), NOW())
				""";
		db.execute(new SqlQuery(sql, List.of(SqlParameter.of(userId), SqlParameter.text(username),
				SqlParameter.of(passwordHash), SqlParameter.of(salt), SqlParameter.of(iterations))));
	}

	@Override
	public Optional<UserRow> findByUsername(final String username) {
		final var sql = """
				SELECT user_id, username, password_hash, salt, iterations, status, created_at, updated_at
				FROM users
				WHERE username = ?
				""";
		return db.queryOne(new SqlQuery(sql, List.of(SqlParameter.text(username))),
				rs -> new UserRow(ResultSets.getUuid(rs, "user_id"), rs.getString("username"),
						rs.getBytes("password_hash"), rs.getBytes("salt"), rs.getInt("iterations"),
						rs.getString("status"), ResultSets.getInstant(rs, "created_at"),
						ResultSets.getInstant(rs, "updated_at")));
	}

	@Override
	public List<String> findRoleNamesByUserId(final UUID userId) {
		return db.queryList(
				new SqlQuery("SELECT role_name FROM api_find_role_names_by_user_id(?::uuid)",
						List.of(SqlParameter.of(userId))),
				rs -> rs.getString(1));
	}

	@Override
	public Optional<UserWithRoles> findByUsernameWithRoles(final String username) {
		final var userOpt = findByUsername(username);
		if (userOpt.isEmpty()) {
			return Optional.empty();
		}
		final var user = userOpt.get();
		final var roles = findRoleNamesByUserId(user.userId());
		return Optional.of(new UserWithRoles(user, roles));
	}

	@Override
	public int countUsers() {
		return db.query(SqlQuery.of("SELECT count(*) FROM users"), rs -> {
			rs.next();
			return rs.getInt(1);
		});
	}
}
