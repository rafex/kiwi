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
import dev.rafex.kiwi.repository.AppClientRepository;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class AppClientRepositoryImpl implements AppClientRepository {

	private final DatabaseClient db;

	public AppClientRepositoryImpl(final DatabaseClient db) {
		this.db = db;
	}

	@Override
	public void createClient(final UUID appClientId, final String clientId, final String name, final byte[] secretHash,
			final byte[] salt, final int iterations, final List<String> roles) {
		final var sql = """
				INSERT INTO app_clients (
					app_client_id, client_id, name, secret_hash, salt, iterations, roles, status, created_at, updated_at
				)
				VALUES (?, ?, ?, ?, ?, ?, ?, 'active', NOW(), NOW())
				""";
		final var rolesArray = normalizeRoles(roles).toArray(new String[0]);
		db.execute(new SqlQuery(sql, List.of(SqlParameter.of(appClientId), SqlParameter.text(clientId),
				SqlParameter.text(name), SqlParameter.of(secretHash), SqlParameter.of(salt),
				SqlParameter.of(iterations), SqlParameter.arrayOf("text", rolesArray))));
	}

	@Override
	public Optional<AppClientRow> findByClientId(final String clientId) {
		final var sql = """
				SELECT app_client_id, client_id, name, secret_hash, salt, iterations,
				       roles, status, last_used_at, created_at, updated_at
				FROM app_clients
				WHERE client_id = ?
				""";
		return db.queryOne(new SqlQuery(sql, List.of(SqlParameter.text(clientId))),
				rs -> new AppClientRow(ResultSets.getUuid(rs, "app_client_id"), rs.getString("client_id"),
						rs.getString("name"), rs.getBytes("secret_hash"), rs.getBytes("salt"), rs.getInt("iterations"),
						toStringList(rs.getArray("roles")), rs.getString("status"),
						ResultSets.getInstant(rs, "last_used_at"), ResultSets.getInstant(rs, "created_at"),
						ResultSets.getInstant(rs, "updated_at")));
	}

	@Override
	public void touchLastUsed(final UUID appClientId) {
		final var sql = """
				UPDATE app_clients
				SET last_used_at = NOW(),
				    updated_at = NOW()
				WHERE app_client_id = ?
				""";
		db.execute(new SqlQuery(sql, List.of(SqlParameter.of(appClientId))));
	}

	private static List<String> normalizeRoles(final List<String> roles) {
		if (roles == null || roles.isEmpty()) {
			return List.of();
		}
		return roles.stream().filter(r -> r != null && !r.isBlank()).map(String::trim).distinct().toList();
	}

	private static List<String> toStringList(final java.sql.Array arr) throws SQLException {
		if (arr == null) {
			return List.of();
		}
		final var raw = arr.getArray();
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof final Object[] values) {
			return Arrays.stream(values).filter(Objects::nonNull).map(String::valueOf).filter(s -> !s.isBlank())
					.map(String::trim).toList();
		}
		return List.of();
	}
}
