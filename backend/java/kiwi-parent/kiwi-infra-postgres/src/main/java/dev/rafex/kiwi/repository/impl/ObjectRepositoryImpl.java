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
import dev.rafex.ether.database.postgres.sql.PostgresParameters;
import dev.rafex.kiwi.query.QuerySpec;
import dev.rafex.kiwi.repository.ObjectRepository;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ObjectRepositoryImpl implements ObjectRepository {

	private final DatabaseClient db;
	private final ObjectQuerySqlBuilder querySqlBuilder = new ObjectQuerySqlBuilder();

	public ObjectRepositoryImpl(final DatabaseClient db) {
		this.db = db;
	}

	@Override
	/**
	 * Crea un nuevo objeto.
	 * Esta operación ejecuta una función PostgreSQL que devuelve VOID.
	 * Se utiliza {@code db.query(..., rs -> null)} para descartar el {@link java.sql.ResultSet}.
	 */
	public void createObject(final UUID objectId, final String name, final String description, final String type,
			final String[] tags, final String metadataJson, final UUID locationId) {
		final var params = new ArrayList<SqlParameter>();
		params.add(SqlParameter.of(objectId));
		params.add(SqlParameter.text(name));
		params.add(description == null ? SqlParameter.nullOf(Types.VARCHAR) : SqlParameter.text(description));
		params.add(type == null ? SqlParameter.nullOf(Types.VARCHAR) : SqlParameter.text(type));
		params.add(tags == null ? SqlParameter.nullOf(Types.ARRAY) : PostgresParameters.textArray(tags));
		params.add(metadataJson == null ? SqlParameter.nullOf(Types.OTHER) : PostgresParameters.jsonb(metadataJson));
		params.add(SqlParameter.of(locationId));
		db.query(new SqlQuery("SELECT api_create_object(?::uuid, ?, ?, ?, ?::text[], ?::jsonb, ?::uuid)", params),
				rs -> null);
	}

	@Override
	/**
	 * Mueve un objeto a una nueva ubicación.
	 * Esta operación ejecuta una función PostgreSQL que devuelve VOID.
	 * Se utiliza {@code db.query(..., rs -> null)} para descartar el {@link java.sql.ResultSet}.
	 */
	public void moveObject(final UUID objectId, final UUID newLocationId) {
		db.query(new SqlQuery("SELECT api_move_object(?::uuid, ?::uuid)",
				List.of(SqlParameter.of(objectId), SqlParameter.of(newLocationId))), rs -> null);
	}

	@Override
	/**
	 * Actualiza las etiquetas de un objeto.
	 * Esta operación ejecuta una función PostgreSQL que devuelve VOID.
	 * Se utiliza {@code db.query(..., rs -> null)} para descartar el {@link java.sql.ResultSet}.
	 */
	public void updateTags(final UUID objectId, final String[] tags) {
		final var tagsParam = tags == null ? SqlParameter.nullOf(Types.ARRAY) : PostgresParameters.textArray(tags);
		db.query(new SqlQuery("SELECT api_update_tags(?::uuid, ?::text[])",
				List.of(SqlParameter.of(objectId), tagsParam)), rs -> null);
	}

	@Override
	/**
	 * Actualiza el nombre y la descripción de un objeto.
	 * Esta operación ejecuta una función PostgreSQL que devuelve VOID.
	 * Se utiliza {@code db.query(..., rs -> null)} para descartar el {@link java.sql.ResultSet}.
	 */
	public void updateText(final UUID objectId, final String name, final String description) {
		db.query(new SqlQuery("SELECT api_update_text(?::uuid, ?, ?)",
				List.of(SqlParameter.of(objectId), SqlParameter.text(name), SqlParameter.text(description))),
				rs -> null);
	}

	@Override
	/**
	 * Actualiza los metadatos de un objeto.
	 * Esta operación ejecuta una función PostgreSQL que devuelve VOID.
	 * Se utiliza {@code db.query(..., rs -> null)} para descartar el {@link java.sql.ResultSet}.
	 */
	public void updateMetadata(final UUID objectId, final String metadataJson) {
		final var metaParam = metadataJson == null
				? SqlParameter.nullOf(Types.OTHER)
				: PostgresParameters.jsonb(metadataJson);
		db.query(new SqlQuery("SELECT api_update_metadata(?::uuid, ?::jsonb)",
				List.of(SqlParameter.of(objectId), metaParam)), rs -> null);
	}

	@Override
	public List<SearchRow> search(final QuerySpec querySpec) {
		final var query = querySqlBuilder.build(querySpec);
		return db.queryList(query,
				rs -> new SearchRow(ResultSets.getUuid(rs, "object_id"), rs.getString("name"), rs.getFloat("rank")));
	}

	@Override
	public List<FuzzyRow> fuzzy(final String text, final int limit, final int offset) {
		return db.queryList(
				new SqlQuery("SELECT object_id, name, score FROM api_fuzzy_search(?, ?, ?)",
						List.of(SqlParameter.text(text), SqlParameter.of(limit), SqlParameter.of(offset))),
				rs -> new FuzzyRow(ResultSets.getUuid(rs, "object_id"), rs.getString("name"), rs.getFloat("score")));
	}

	@Override
	public Optional<ObjectDetailRow> findById(final UUID objectId) {
		final var sql = "SELECT object_id, name, description, type, status, current_location_id, "
				+ "tags, metadata::text AS metadata, created_at, updated_at FROM api_get_object(?::uuid)";
		return db.queryOne(new SqlQuery(sql, List.of(SqlParameter.of(objectId))), rs -> {
			final var tags = ResultSets.getStringArray(rs, "tags");
			return new ObjectDetailRow(ResultSets.getUuid(rs, "object_id"), rs.getString("name"),
					rs.getString("description"), rs.getString("type"), rs.getString("status"),
					ResultSets.getUuid(rs, "current_location_id"), tags, rs.getString("metadata"),
					ResultSets.getInstant(rs, "created_at"), ResultSets.getInstant(rs, "updated_at"));
		});
	}
}
