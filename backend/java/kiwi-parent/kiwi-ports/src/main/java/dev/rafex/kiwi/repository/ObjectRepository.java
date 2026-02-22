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
package dev.rafex.kiwi.repository;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ObjectRepository {

	void createObject(UUID objectId, String name, String description, String type, String[] tags, String metadataJson,
			UUID locationId) throws SQLException;

	void moveObject(UUID objectId, UUID newLocationId) throws SQLException;

	void updateTags(UUID objectId, String[] tags) throws SQLException;

	void updateText(UUID objectId, String name, String description) throws SQLException;

	void updateMetadata(UUID objectId, String metadataJson) throws SQLException;

	Optional<ObjectDetailRow> findById(UUID objectId) throws SQLException;

	List<SearchRow> search(String query, String[] tags, UUID locationId, int limit) throws SQLException;

	List<FuzzyRow> fuzzy(String text, int limit) throws SQLException;

	record SearchRow(UUID objectId, String name, float rank) {
	}

	record FuzzyRow(UUID objectId, String name, float score) {
	}

	record ObjectDetailRow(UUID objectId, String name, String description, String type, String status,
			UUID currentLocationId, String[] tags, String metadataJson, Instant createdAt, Instant updatedAt) {
	}

}