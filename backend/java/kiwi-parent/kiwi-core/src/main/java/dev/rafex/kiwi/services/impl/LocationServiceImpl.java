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

import dev.rafex.kiwi.errors.KiwiError;
import dev.rafex.kiwi.logging.Log;
import dev.rafex.kiwi.repository.LocationRepository;
import dev.rafex.kiwi.services.LocationService;

import java.sql.SQLException;
import java.util.UUID;

public class LocationServiceImpl implements LocationService {

	private final LocationRepository repo;

	public LocationServiceImpl(final LocationRepository repo) {
		this.repo = repo;
	}

	@Override
	public void create(final UUID locationId, final String name, final UUID parentId) throws KiwiError {
		try {
			repo.createLocation(locationId, name, parentId);
		} catch (final SQLException e) {
			Log.debug(getClass(), "DB error creating location: {} ", e.getMessage());
			if ("23503".equals(e.getSQLState())) {
				throw new KiwiError("E-001", "newLocationId does not exist", e);
			}
			throw new KiwiError("E-002", "DB error creating location", e);
		}
	}
}
