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
package dev.rafex.kiwi.services;

import dev.rafex.kiwi.errors.KiwiError;
import dev.rafex.kiwi.models.FuzzyItem;
import dev.rafex.kiwi.models.SearchItem;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ObjectService {

	void create(UUID objectId, String name, String description, String type, String[] tags, String metadataJson,
			UUID locationId) throws Exception;

	void move(UUID objectId, UUID newLocationId) throws KiwiError;

	List<SearchItem> search(String query, String[] tags, UUID locationId, int limit);

	void updateTags(UUID objectId, String[] tags) throws KiwiError;

	void updateText(UUID objectId, String name, String description) throws KiwiError;

	List<FuzzyItem> fuzzy(String text, int limit) throws Exception;

	Optional<ObjectDetail> getById(UUID objectId) throws Exception;

	record ObjectDetail(UUID objectId, String name, String description, String type, String status,
			UUID currentLocationId, String[] tags, String metadataJson, Instant createdAt, Instant updatedAt) {
	}
}
