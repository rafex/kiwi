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
import dev.rafex.ether.database.core.sql.SqlParameter;
import dev.rafex.ether.database.core.sql.SqlQuery;
import dev.rafex.kiwi.repository.LocationRepository;

import java.sql.Types;
import java.util.List;
import java.util.UUID;

public class LocationRepositoryImpl implements LocationRepository {

	private final DatabaseClient db;

	public LocationRepositoryImpl(final DatabaseClient db) {
		this.db = db;
	}

	@Override
	public void createLocation(final UUID locationId, final String name, final UUID parentLocationId) {
		final var parentParam = parentLocationId == null
				? SqlParameter.nullOf(Types.OTHER)
				: SqlParameter.of(parentLocationId);
		db.query(new SqlQuery("SELECT api_create_location(?::uuid, ?, ?::uuid)",
				List.of(SqlParameter.of(locationId), SqlParameter.text(name), parentParam)), rs -> null);
	}

	@Override
	public boolean locationExists(final UUID locationId) {
		return db.queryOne(new SqlQuery("SELECT 1 FROM locations WHERE location_id = ?::uuid",
				List.of(SqlParameter.of(locationId))), rs -> rs.getInt(1)).isPresent();
	}
}
