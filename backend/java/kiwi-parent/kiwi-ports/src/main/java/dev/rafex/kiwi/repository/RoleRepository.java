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
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository {

	Optional<RoleRow> findByName(String name) throws SQLException;

	UUID ensureRole(final String name, final String description) throws SQLException;

	void assignRoleToUser(UUID userId, UUID roleId) throws SQLException;

	public record RoleRow(UUID roleId, String name, String description, String status, Instant createdAt,
			Instant updatedAt) {
	}
}
