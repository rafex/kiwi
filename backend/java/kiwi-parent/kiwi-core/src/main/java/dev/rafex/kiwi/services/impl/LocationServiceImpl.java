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

import dev.rafex.ether.database.core.exceptions.DatabaseAccessException;
import dev.rafex.ether.database.postgres.errors.PostgresErrorClassifier;
import dev.rafex.kiwi.errors.KiwiError;
import dev.rafex.kiwi.logging.Log;
import dev.rafex.kiwi.repository.LocationRepository;
import dev.rafex.kiwi.services.LocationService;

import java.util.UUID;

/**
 * Implementación del servicio de gestión de ubicaciones.
 * Maneja la creación de ubicaciones jerárquicas y valida restricciones de integridad referencial.
 */
public class LocationServiceImpl implements LocationService {

	private final LocationRepository repo;

	/**
	 * Crea una instancia de LocationServiceImpl.
	 *
	 * @param repo repositorio de ubicaciones para operaciones de persistencia
	 */
	public LocationServiceImpl(final LocationRepository repo) {
		this.repo = repo;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void create(final UUID locationId, final String name, final UUID parentId) throws KiwiError {
		try {
			repo.createLocation(locationId, name, parentId);
		} catch (final DatabaseAccessException e) {
			Log.debug(getClass(), "DB error creating location: {} ", e.getMessage());
			if (e.getCause() instanceof final java.sql.SQLException sqle
					&& PostgresErrorClassifier.Category.FOREIGN_KEY_VIOLATION == PostgresErrorClassifier
							.classify(sqle)) {
				throw new KiwiError("E-001", "newLocationId does not exist", e);
			}
			throw new KiwiError("E-002", "DB error creating location", e);
		}
	}
}
