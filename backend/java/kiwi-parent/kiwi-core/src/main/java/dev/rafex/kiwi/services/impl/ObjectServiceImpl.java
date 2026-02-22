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
import dev.rafex.kiwi.models.FuzzyItem;
import dev.rafex.kiwi.models.SearchItem;
import dev.rafex.kiwi.repository.ObjectRepository;
import dev.rafex.kiwi.services.ObjectService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ObjectServiceImpl implements ObjectService {

	private final ObjectRepository repo;

	public ObjectServiceImpl(final ObjectRepository repo) {
		this.repo = repo;
	}

	@Override
	public void create(final UUID objectId, final String name, final String description, final String type,
			final String[] tags, final String metadataJson, final UUID locationId) throws KiwiError {
		try {
			repo.createObject(objectId, name, description, type, tags, metadataJson, locationId);
		} catch (final SQLException e) {
			Log.error(getClass(), "Error creating object in DB", e);
			throw new KiwiError("E-003", "Error creating object in DB", e);
		}
	}

	@Override
	public void move(final UUID objectId, final UUID newLocationId) throws KiwiError {
		try {
			repo.moveObject(objectId, newLocationId);
		} catch (final SQLException e) {
			Log.error(getClass(), "DB error moving object", e);
			if ("23503".equals(e.getSQLState())) {
				throw new KiwiError("E-001", "newLocationId does not exist", e);
			}
			throw new KiwiError("E-002", "DB error moving object", e);
		}

	}

	@Override
	public List<SearchItem> search(final String query, final String[] tags, final UUID locationId, final int limit) {
		try {
			final var rows = repo.search(query, tags, locationId, limit);
			// Si SearchRow y SearchItem comparten campos, usar view directa
			final var result = new ArrayList<SearchItem>(rows.size());
			for (final var r : rows) {
				result.add(new SearchItem(r.objectId(), r.name(), r.rank()));
			}
			return result;
		} catch (final SQLException e) {
			Log.error(getClass(), "Error searching for objects", e);
			return List.of(); // immutable empty, zero-alloc
		}
	}

	@Override
	public void updateTags(final UUID objectId, final String[] tags) throws KiwiError {
		try {
			repo.updateTags(objectId, tags);
		} catch (final SQLException e) {
			Log.error(getClass(), "Error updating tags", e);
			throw new KiwiError("E-004", "Error updating tags", e);
		}
	}

	@Override
	public void updateText(final UUID objectId, final String name, final String description) throws KiwiError {
		try {
			repo.updateText(objectId, name, description);
		} catch (final SQLException e) {
			Log.error(getClass(), "Error updating text fields", e);
			throw new KiwiError("E-005", "Error updating text fields", e);
		}
	}

	@Override
	public List<FuzzyItem> fuzzy(final String text, final int limit) throws KiwiError {
		try {
			final var rows = repo.fuzzy(text, limit);
			final var result = new ArrayList<FuzzyItem>(rows.size());
			for (final var r : rows) {
				result.add(new FuzzyItem(r.objectId(), r.name(), r.score()));
			}
			return result;
		} catch (final SQLException e) {
			Log.error(getClass(), "Error performing fuzzy search", e);
			// Decide si lanzar error o retornar vacío
			return List.of();
		}
	}

	@Override
	public Optional<ObjectDetail> getById(final UUID objectId) throws KiwiError {
		try {
			final var rowOpt = repo.findById(objectId);
			if (rowOpt.isEmpty()) {
				return Optional.empty();
			}
			final var row = rowOpt.get();
			return Optional.of(new ObjectDetail(row.objectId(), row.name(), row.description(), row.type(), row.status(),
					row.currentLocationId(), row.tags(), row.metadataJson(), row.createdAt(), row.updatedAt()));
		} catch (final SQLException e) {
			Log.error(getClass(), "Error obtaining object by id", e);
			throw new KiwiError("E-006", "Error obtaining object by id", e);
		}
	}

}