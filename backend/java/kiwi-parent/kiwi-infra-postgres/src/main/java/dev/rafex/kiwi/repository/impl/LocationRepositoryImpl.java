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

import dev.rafex.kiwi.repository.LocationRepository;

import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

import javax.sql.DataSource;

public class LocationRepositoryImpl implements LocationRepository {

	private final DataSource ds;

	public LocationRepositoryImpl(final DataSource ds) {
		this.ds = ds;
	}

	@Override
	public void createLocation(final UUID locationId, final String name, final UUID parentLocationId)
			throws SQLException {
		try (var c = ds.getConnection();
				var ps = c.prepareStatement("SELECT api_create_location(?::uuid, ?, ?::uuid)")) {

			ps.setObject(1, locationId);
			ps.setString(2, name);

			if (parentLocationId == null) {
				ps.setNull(3, Types.OTHER);
			} else {
				ps.setObject(3, parentLocationId);
			}

			ps.execute();
		}
	}

	// TODO falta implementar
	@Override
	public boolean locationExists(final UUID locationId) throws SQLException {
		try (var c = ds.getConnection();
				var ps = c.prepareStatement("SELECT 1 FROM locations WHERE location_id = ?::uuid")) {
			ps.setObject(1, locationId);
			try (var rs = ps.executeQuery()) {
				return rs.next();
			}
		}
	}

}
