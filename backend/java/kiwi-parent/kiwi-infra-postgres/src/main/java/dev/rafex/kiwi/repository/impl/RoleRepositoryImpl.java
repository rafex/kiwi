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
import dev.rafex.ether.database.core.exceptions.DatabaseAccessException;
import dev.rafex.ether.database.core.mapping.ResultSets;
import dev.rafex.ether.database.core.sql.SqlParameter;
import dev.rafex.ether.database.core.sql.SqlQuery;
import dev.rafex.ether.database.postgres.errors.PostgresErrorClassifier;
import dev.rafex.kiwi.repository.RoleRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RoleRepositoryImpl implements RoleRepository {

	private final DatabaseClient db;

	public RoleRepositoryImpl(final DatabaseClient db) {
		this.db = db;
	}

	@Override
	public Optional<RoleRow> findByName(final String name) {
		final var sql = """
				SELECT role_id, name, description, status, created_at, updated_at
				FROM roles
				WHERE name = ?
				""";
		return db.queryOne(new SqlQuery(sql, List.of(SqlParameter.text(name))),
				rs -> new RoleRow(ResultSets.getUuid(rs, "role_id"), rs.getString("name"),
						rs.getString("description"), rs.getString("status"), ResultSets.getInstant(rs, "created_at"),
						ResultSets.getInstant(rs, "updated_at")));
	}

	@Override
	public UUID ensureRole(final String name, final String description) {
		final var existing = db.queryOne(
				new SqlQuery("SELECT role_id FROM roles WHERE name = ?", List.of(SqlParameter.text(name))),
				rs -> ResultSets.getUuid(rs, "role_id"));
		if (existing.isPresent()) {
			return existing.get();
		}

		final var roleId = UUID.randomUUID();
		try {
			db.execute(new SqlQuery("INSERT INTO roles (role_id, name, description, status) VALUES (?, ?, ?, 'active')",
					List.of(SqlParameter.of(roleId), SqlParameter.text(name), SqlParameter.text(description))));
			return roleId;
		} catch (final DatabaseAccessException e) {
			if (e.getCause() instanceof final SQLException sqle
					&& PostgresErrorClassifier.Category.UNIQUE_VIOLATION == PostgresErrorClassifier.classify(sqle)) {
				// Race condition: another thread created the same role concurrently
				return db.queryOne(
						new SqlQuery("SELECT role_id FROM roles WHERE name = ?", List.of(SqlParameter.text(name))),
						rs -> ResultSets.getUuid(rs, "role_id")).orElseThrow(
								() -> new DatabaseAccessException("Role not found after insert conflict", e));
			}
			throw e;
		}
	}

	@Override
	public void assignRoleToUser(final UUID userId, final UUID roleId) {
		db.execute(new SqlQuery("SELECT api_assign_role_to_user(?::uuid, ?::uuid)",
				List.of(SqlParameter.of(userId), SqlParameter.of(roleId))));
	}
}
